document.getElementById('loginForm').addEventListener('submit', async (e) => {
    e.preventDefault();

    const username = document.getElementById('username').value;
    const password = document.getElementById('password').value;
    const errorMessage = document.getElementById('errorMessage');

    errorMessage.style.display = 'none';

    try {
        const response = await fetch('/auth/login', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json'
            },
            body: JSON.stringify({ username, password })
        });

        const data = await response.json();

        if (response.ok && data.token) {
            localStorage.setItem('token', data.token);
            localStorage.setItem('username', username);
            window.location.href = '/';
        } else {
            errorMessage.textContent = data.error || '登录失败，请检查用户名和密码';
            errorMessage.style.display = 'block';
        }
    } catch (error) {
        errorMessage.textContent = '网络错误，请稍后重试';
        errorMessage.style.display = 'block';
    }
});
