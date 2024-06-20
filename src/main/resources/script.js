let board = document.getElementById('board');
let stompClient = null;
let selectedPiece = null;
let moveSound = document.getElementById('moveSound');
let currentTeam = "A"; // 이 값은 실제 게임에서 사용자가 설정하는 방식에 따라 달라질 수 있습니다.
let currentRole = "COMMANDER"; // 이 값도 마찬가지입니다.

const pieceImages = {
    'WHITE_PAWN': 'images/wp.png',
    'WHITE_ROOK': 'images/wr.png',
    'WHITE_KNIGHT': 'images/wn.png',
    'WHITE_BISHOP': 'images/wb.png',
    'WHITE_QUEEN': 'images/wq.png',
    'WHITE_KING': 'images/wk.png',
    'BLACK_PAWN': 'images/bp.png',
    'BLACK_ROOK': 'images/br.png',
    'BLACK_KNIGHT': 'images/bn.png',
    'BLACK_BISHOP': 'images/bb.png',
    'BLACK_QUEEN': 'images/bq.png',
    'BLACK_KING': 'images/bk.png'
};

function createBoard() {
    for (let y = 7; y >= 0; y--) {
        for (let x = 0; x < 8; x++) {
            let square = document.createElement('div');
            square.classList.add('square', (x + y) % 2 === 0 ? 'light' : 'dark');
            square.dataset.x = x;
            square.dataset.y = y;
            square.addEventListener('dragover', dragOver);
            square.addEventListener('drop', drop);
            board.appendChild(square);
        }
    }
}

function renderBoard(gameState) {
    document.querySelectorAll('.square').forEach(square => square.innerHTML = '');
    for (let position in gameState.board) {
        let piece = gameState.board[position];
        let square = document.querySelector(`.square[data-x="${position.x}"][data-y="${position.y}"]`);
        let img = document.createElement('img');
        img.src = pieceImages[`${piece.color}_${piece.type}`];
        img.classList.add('piece');
        img.setAttribute('draggable', 'true');
        img.addEventListener('dragstart', dragStart);
        square.appendChild(img);
    }
}

function connect() {
    let socket = new SockJS('/chess-game');
    stompClient = Stomp.over(socket);
    stompClient.connect({}, function (frame) {
        console.log('Connected: ' + frame);
        stompClient.subscribe('/topic/gameState', function (message) {
            renderBoard(JSON.parse(message.body));
        });
        stompClient.send("/app/reset", {});
    });
}

function dragStart(event) {
    selectedPiece = {
        x: event.target.parentElement.dataset.x,
        y: event.target.parentElement.dataset.y
    };
}

function dragOver(event) {
    event.preventDefault();
}

function drop(event) {
    if (selectedPiece) {
        let targetX = event.target.dataset.x;
        let targetY = event.target.dataset.y;
        stompClient.send("/app/move", {}, JSON.stringify({
            move: { from: selectedPiece, to: { x: targetX, y: targetY } },
            team: currentTeam,
            role: currentRole
        }));
        moveSound.play();
        selectedPiece = null;
    }
}

createBoard();
connect();
