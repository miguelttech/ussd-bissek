const messageEl = document.getElementById('ussd-message');
const inputEl = document.getElementById('ussd-input');
const sendBtn = document.getElementById('send-btn');

// let sessionId = "SIM-001"; // identifiant de session simulée
// let phoneNumber = "+237600000000"; // numéro fictif

async function sendInput() {
    const input = inputEl.value.trim();
    if (!input) return;

    const response = await fetch('/api/ussd/', {
        method: 'POST',
        headers: {'Content-Type': 'application/json'},
        body: JSON.stringify({ sessionId, msisdn: phoneNumber, input })
    });

    const data = await response.json();
    messageEl.textContent = data.message;
    inputEl.value = '';

    if (data.type === 'END') {
        inputEl.disabled = true;
        sendBtn.disabled = true;
    }
}

sendBtn.addEventListener('click', sendInput);
inputEl.addEventListener('keydown', (e) => {
    if (e.key === 'Enter') sendInput();
});
