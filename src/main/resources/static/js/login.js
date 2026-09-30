document.getElementById("login-form").addEventListener("submit", async function (event) {
    event.preventDefault();

    const username = document.getElementById("username").value.trim();
    const password = document.getElementById("password").value;
    const errorMessage = document.getElementById("error-message");
    const button = document.getElementById("login-button");

    errorMessage.textContent = "";
    button.disabled = true;

    try {
        const response = await fetch("/api/auth/login", {
            method: "POST",
            headers: { "Content-Type": "application/json", "Accept": "application/json" },
            body: JSON.stringify({ username, password })
        });

        if (!response.ok) {
            let message = "Usuário ou senha inválidos.";
            try {
                const error = await response.json();
                if (error.fields && Object.keys(error.fields).length > 0) {
                    message = Object.values(error.fields).join(" ");
                } else if (error.message) {
                    message = error.message;
                }
            } catch (err) {
                // resposta sem JSON, usa a mensagem padrão
            }
            errorMessage.textContent = message;
            return;
        }

        window.location.href = "/fichas";
    } catch (err) {
        errorMessage.textContent = "Não foi possível conectar ao servidor.";
    } finally {
        button.disabled = false;
    }
});
