const chatDiv = document.getElementById('chat-div')
const responseDiv = document.getElementById('response')
const senderInput = document.getElementById('sender')
const messageInput = document.getElementById('message')
const roomIdInput = document.getElementById('room-id')
const userLoginDiv = document.getElementById('user-login')
const roomTitle = document.getElementById('room-title')
const sessionMeta = document.getElementById('session-meta')
const isTypingDiv = document.getElementById('is-typing')
const noticeDiv = document.getElementById('notice')
const loginFeedback = document.getElementById('login-feedback')
const loginForm = document.getElementById('login-form')
const createRoomButton = document.getElementById('create-room')
const joinRoomForm = document.getElementById('join-room-form')
const leaveRoomButton = document.getElementById('leave-room')
const disconnectButton = document.getElementById('disconnect')
const composerForm = document.getElementById('composer-form')

let stompClient = null
let sender = null
let currentRoom = 'global'
let currentSub = null
let errorSub = null
let typingTimeout = null
let noticeTimeout = null

function setConnected(connected) {
    userLoginDiv.hidden = connected
    chatDiv.hidden = !connected
    responseDiv.innerHTML = ''
    isTypingDiv.textContent = ''
    setNotice('')
}

function connect() {
    sender = senderInput.value.trim()
    if (!sender) {
        setLoginFeedback('Choose a nickname before connecting.', 'error')
        senderInput.focus()
        return
    }

    if (typeof SockJS === 'undefined' || typeof Stomp === 'undefined') {
        setLoginFeedback('Chat libraries did not load. Check your connection and refresh.', 'error')
        return
    }

    setLoginFeedback('Connecting...', '')
    let socket = new SockJS('/ws')
    stompClient = Stomp.over(socket)
    stompClient.connect({}, function() {
        setConnected(true)
        setLoginFeedback('', '')
        errorSub = stompClient.subscribe('/user/queue/errors', function(incomingMessage) {
            displayError(JSON.parse(incomingMessage.body))
        })
        goToRoom('global')
    }, function() {
        setLoginFeedback('Could not connect to the server.', 'error')
    })
}

function disconnect() {
    if (currentRoom !== 'global')
        leaveCurrentRoom()

    if (errorSub)
        errorSub.unsubscribe()
    if (stompClient != null)
        stompClient.disconnect()

    errorSub = null
    currentSub = null
    stompClient = null
    currentRoom = 'global'
    roomIdInput.value = ''
    setConnected(false)
}

function sendMessage() {
    let msgText = messageInput.value.trim()
    if (!msgText)
        return

    stompClient.send(`/app/chat/rooms/${currentRoom}/messages`, {},
        JSON.stringify({'message': msgText }))
    messageInput.value = ''
}

function sendTypingStatus() {
    if (stompClient && currentRoom !== 'global')
        stompClient.send(`/app/chat/rooms/${currentRoom}/typing`, {}, JSON.stringify({}))
}

function displayMessage(incomingMessage) {
    switch (incomingMessage.type) {
        case 'CHAT':
            if (isTypingDiv.textContent.includes(incomingMessage.sender))
                isTypingDiv.textContent = ''
            appendChatMessage(incomingMessage)
            break
        case 'TYPING':
            if (incomingMessage.sender !== sender) {
                isTypingDiv.textContent = `${incomingMessage.sender} is typing...`
                clearTimeout(typingTimeout)
                typingTimeout = setTimeout(() => { isTypingDiv.textContent = '' }, 5000)
            }
            break
        case 'JOIN':
            if (currentRoom !== 'global')
                appendSystemMessage(`${incomingMessage.sender} joined the room`)
            break
        case 'LEAVE':
            if (currentRoom !== 'global')
                appendSystemMessage(`${incomingMessage.sender} left the room`)
            break
    }
}

function appendChatMessage(incomingMessage) {
    let article = document.createElement('article')
    article.className = incomingMessage.sender === sender ? 'message own' : 'message'

    let senderSpan = document.createElement('span')
    senderSpan.className = 'message-sender'
    senderSpan.textContent = incomingMessage.sender === sender ? 'You' : incomingMessage.sender

    let textSpan = document.createElement('span')
    textSpan.className = 'message-text'
    textSpan.textContent = incomingMessage.message

    article.appendChild(senderSpan)
    article.appendChild(textSpan)
    responseDiv.appendChild(article)
    scrollMessagesToBottom()
}

function appendSystemMessage(message) {
    let p = document.createElement('p')
    p.className = 'message system'
    p.textContent = message
    responseDiv.appendChild(p)
    scrollMessagesToBottom()
}

function displayError(error) {
    setNotice(error.message, 'error')
    if (error.action === 'join' && error.roomId === currentRoom && currentRoom !== 'global')
        goToRoom('global')
}

function goToRoom(room) {
    if (currentSub)
        currentSub.unsubscribe()

    responseDiv.innerHTML = ''
    isTypingDiv.textContent = ''
    currentRoom = room
    updateRoomUi()

    currentSub = stompClient.subscribe(`/topic/rooms/${room}`, function(incomingMessage) {
        displayMessage(JSON.parse(incomingMessage.body))
    })

    stompClient.send(`/app/chat/rooms/${room}/join`, {},
        JSON.stringify({'nickname': sender }))
    messageInput.focus()
}

function leaveCurrentRoom() {
    if (!stompClient || currentRoom === 'global')
        return

    stompClient.send(`/app/chat/rooms/${currentRoom}/leave`, {}, JSON.stringify({}))
}

function backToGlobal() {
    leaveCurrentRoom()
    roomIdInput.value = ''
    goToRoom('global')
}

async function createRoom() {
    setNotice('Creating private room...', '')
    try {
        const response = await fetch('/api/rooms', { method: 'POST' })
        if (!response.ok)
            throw new Error('Room creation failed')

        const room = await response.text()
        roomIdInput.value = room
        setNotice(`Private room created: ${room}`, 'success')
        goToRoom(room)
    }
    catch (error) {
        setNotice('Error creating a room.', 'error')
    }
}

async function joinRoom() {
    let room = roomIdInput.value.trim()
    if (!room) {
        setNotice('Enter a private room ID first.', 'error')
        roomIdInput.focus()
        return
    }

    setNotice('Checking room...', '')
    try {
        const available = await fetch(`/api/rooms/${room}/available`)
        if (available.ok) {
            goToRoom(room)
            setNotice('', '')
        }
        else {
            setNotice('Invalid room ID.', 'error')
        }
    }
    catch (error) {
        setNotice('Error validating room.', 'error')
    }
}

function updateRoomUi() {
    let isGlobal = currentRoom === 'global'
    roomTitle.textContent = isGlobal ? 'Public Chat' : `Room ${currentRoom}`
    sessionMeta.textContent = `${sender} connected`
    createRoomButton.hidden = !isGlobal
    joinRoomForm.hidden = !isGlobal
    leaveRoomButton.hidden = isGlobal
}

function setLoginFeedback(message, type) {
    loginFeedback.textContent = message
    loginFeedback.className = type ? `inline-feedback ${type}` : 'inline-feedback'
}

function setNotice(message, type) {
    clearTimeout(noticeTimeout)
    noticeDiv.textContent = message
    noticeDiv.className = type ? `notice ${type}` : 'notice'

    if (message && type !== 'error')
        noticeTimeout = setTimeout(() => setNotice('', ''), 5000)
}

function scrollMessagesToBottom() {
    responseDiv.scrollTop = responseDiv.scrollHeight
}

loginForm.addEventListener('submit', function(event) {
    event.preventDefault()
    connect()
})

joinRoomForm.addEventListener('submit', function(event) {
    event.preventDefault()
    joinRoom()
})

composerForm.addEventListener('submit', function(event) {
    event.preventDefault()
    sendMessage()
})

messageInput.addEventListener('input', sendTypingStatus)
createRoomButton.addEventListener('click', createRoom)
leaveRoomButton.addEventListener('click', function() {
    backToGlobal()
})
disconnectButton.addEventListener('click', disconnect)

setConnected(false)
