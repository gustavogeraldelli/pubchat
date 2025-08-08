const chatDiv = document.getElementById('chat-div')
const responseDiv = document.getElementById('response')
const senderInput = document.getElementById('sender')
const messageInput = document.getElementById('message')
const userLoginDiv = document.getElementById('user-login')
const roomTitle = document.getElementById('room-title')
const isTypingDiv = document.getElementById('is-typing')

let stompClient = null
let sender = null
let currentRoom = 'global'
let currentSub = null
let typingTimeout = null

function setConnected(connected) {
    if (connected) {
        userLoginDiv.style.display = 'none'
        chatDiv.style.display = 'block'
    } else {
        userLoginDiv.style.display = 'block'
        chatDiv.style.display = 'none'
    }
    responseDiv.innerHTML = ''
}

function connect() {
    sender = senderInput.value.trim()
    if (!sender) {
        alert("Please, choose a nickname")
        return
    }
    let socket = new SockJS('/pubchat-websocket')
    stompClient = Stomp.over(socket)
    stompClient.connect({}, function() {
        setConnected(true)
        goToRoom('global')
    })
}

function disconnect() {
    if (stompClient != null)
        stompClient.disconnect()
    setConnected(false)
    console.log("Disconnected")
}

function sendMessage() {
    let msgText = messageInput.value.trim()
    if (msgText) {
        stompClient.send(`/pubchat/room/${currentRoom}`, {},
            JSON.stringify({'sender': sender, 'message': msgText, 'type': 'CHAT' }))
        messageInput.value = ''
    }
}

function sendTypingStatus() {
    if (stompClient && currentRoom !== 'global')
        stompClient.send(`/pubchat/typing/${currentRoom}`, {},
            JSON.stringify({'sender': sender, 'type': 'TYPING'}))
}

function displayMessage(incomingMessage) {
    switch (incomingMessage.type) {
        case 'CHAT':
            if (isTypingDiv.textContent.includes(incomingMessage.sender))
                isTypingDiv.textContent = ''
            let p = document.createElement('p')
            p.textContent = `${incomingMessage.sender}: ${incomingMessage.message}`
            responseDiv.appendChild(p)
            responseDiv.scrollTop = responseDiv.scrollHeight
            break
        case 'TYPING':
            if (incomingMessage.sender !== sender) {
                isTypingDiv.textContent = `${incomingMessage.sender} is typing...`
                clearTimeout(typingTimeout)
                typingTimeout = setTimeout(() => { isTypingDiv.textContent = ''; }, 5000)
            }
    }
}

function goToRoom(room) {
    if (currentSub)
        currentSub.unsubscribe()
    responseDiv.innerHTML = '' // since the url stays the same, the old chat messages needs to be cleaned
    isTypingDiv.textContent = ''

    if (room === 'global') {
        roomTitle.textContent = 'Public Chat'
        document.getElementById('leave-room').style.visibility = 'hidden'
        document.getElementById('create-room').style.visibility = 'visible'
        document.getElementById('join-room').style.visibility = 'visible'
    }
    else {
        roomTitle.textContent = `Room ${room}`
        document.getElementById('leave-room').style.visibility = 'visible'
        document.getElementById('create-room').style.visibility = 'hidden'
        document.getElementById('join-room').style.visibility = 'hidden'
    }
    currentRoom = room
    currentSub = stompClient.subscribe(`/topic/room/${room}`, function (incomingMessage) {
        displayMessage(JSON.parse(incomingMessage.body))
    })
}

async function createRoom() {
    try {
        const response = await fetch('/pubchat/rooms', { method: 'POST' })
        const room = await response.text()
        alert("Private room created!\nID: " + room)
        goToRoom(room)
    }
    catch (error) {
        alert("Error creating a room")
    }
}

async function joinRoom() {
    let room = prompt("Enter the room ID: ")
    if (room && room.trim() !== '') {
        room = room.trim()
        try {
            // users cant use any id to join private rooms
            const available = await fetch(`/pubchat/rooms/${room}/available`)
            if (available.ok)
                goToRoom(room)
            else
                alert("Invalid ID")
        }
        catch (error) {
            alert("Error validating room")
        }
    }
}

setConnected(false)