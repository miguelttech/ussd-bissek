const messageEl = document.getElementById('ussd-message');
const inputEl = document.getElementById('ussd-input');
const sendBtn = document.getElementById('send-btn');

let sessionId = 'TEST_' + Date.now();
let textHistory = '';

async function callUssd(userInput = null) {
    try {
        // Construire l'historique (format Africa's Talking: "1*2*5")
        if (userInput) {
            textHistory = textHistory ? `${textHistory}*${userInput}` : userInput;
        }
        
        const payload = {
            sessionId: sessionId,
            phoneNumber: '+237670123456',
            text: textHistory,
            serviceCode: '*384*1#',
            networkCode: '62120'
        };
        
        console.log('📤 Requête:', payload);
        
        // Appeler l'endpoint principal (retourne du texte brut)
        const response = await fetch('/api/ussd', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(payload)
        });
        
        // Récupérer la réponse en texte brut
        const text = await response.text();
        console.log('📥 Réponse:', text);
        
        // Afficher directement dans le div
        messageEl.textContent = text;
        
        // Vérifier si c'est la fin (commence par "END")
        const isEnd = text.startsWith('END');
        
        if (isEnd) {
            inputEl.disabled = true;
            sendBtn.disabled = true;
            messageEl.style.color = '#4CAF50';
        } else {
            inputEl.disabled = false;
            sendBtn.disabled = false;
            inputEl.focus();
        }
        
        inputEl.value = '';
        
    } catch (error) {
        console.error('❌ Erreur:', error);
        messageEl.textContent = 'Erreur de connexion: ' + error.message;
        messageEl.style.color = '#f44336';
    }
}

async function sendInput() {
    const userInput = inputEl.value.trim();
    if (!userInput) return;
    await callUssd(userInput);
}

function resetSession() {
    sessionId = 'TEST_' + Date.now();
    textHistory = '';
    inputEl.disabled = false;
    sendBtn.disabled = false;
    messageEl.style.color = '#333';
    messageEl.textContent = 'Chargement...';
    callUssd();
}

// INITIALISATION
window.addEventListener('DOMContentLoaded', () => {
    callUssd(); // Première requête (text vide)
});

sendBtn.addEventListener('click', sendInput);

inputEl.addEventListener('keydown', (e) => {
    if (e.key === 'Enter') sendInput();
});

// Ajouter un bouton reset (optionnel)
const resetBtn = document.createElement('button');
resetBtn.textContent = '🔄 Recommencer';
resetBtn.style.marginTop = '10px';
resetBtn.onclick = resetSession;
document.body.appendChild(resetBtn);