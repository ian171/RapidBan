// 检查登录状态
const token = localStorage.getItem('token');
const username = localStorage.getItem('username');

if (!token) {
    window.location.href = '/login.html';
}

// 显示用户信息
document.getElementById('userInfo').textContent = `当前用户: ${username}`;

// API 请求封装
async function apiRequest(url, options = {}) {
    const headers = {
        'Authorization': `Bearer ${token}`,
        'Content-Type': 'application/json',
        ...options.headers
    };

    try {
        const response = await fetch(url, { ...options, headers });

        if (response.status === 401) {
            localStorage.removeItem('token');
            localStorage.removeItem('username');
            window.location.href = '/login.html';
            return null;
        }

        return await response.json();
    } catch (error) {
        console.error('API Error:', error);
        alert('网络错误，请稍后重试');
        return null;
    }
}

// 页面导航
document.querySelectorAll('.nav-menu a').forEach(link => {
    link.addEventListener('click', (e) => {
        e.preventDefault();
        const pageName = e.target.dataset.page;

        document.querySelectorAll('.nav-menu a').forEach(a => a.classList.remove('active'));
        e.target.classList.add('active');

        document.querySelectorAll('.page').forEach(page => page.classList.remove('active'));
        document.getElementById(pageName + 'Page').classList.add('active');

        if (pageName === 'dashboard') {
            loadDashboard();
        } else if (pageName === 'punishments') {
            loadAllPunishments();
        }
    });
});

// 退出登录
document.getElementById('logoutBtn').addEventListener('click', () => {
    localStorage.removeItem('token');
    localStorage.removeItem('username');
    window.location.href = '/login.html';
});

// 加载仪表盘
async function loadDashboard() {
    const recentData = await apiRequest('/api/stats/recent');

    if (recentData) {
        document.getElementById('totalPunishments').textContent = recentData.total || 0;
        document.getElementById('activeBans').textContent = recentData.active || 0;
        document.getElementById('todayPunishments').textContent = recentData.today || 0;
        document.getElementById('totalPlayers').textContent = recentData.players || 0;

        displayRecentPunishments(recentData.recent || []);
    }
}

function displayRecentPunishments(punishments) {
    const container = document.getElementById('recentPunishments');

    if (punishments.length === 0) {
        container.innerHTML = '<p class="loading">暂无处罚记录</p>';
        return;
    }

    const table = `
        <table>
            <thead>
                <tr>
                    <th>玩家</th>
                    <th>类型</th>
                    <th>原因</th>
                    <th>时间</th>
                    <th>状态</th>
                </tr>
            </thead>
            <tbody>
                ${punishments.map(p => `
                    <tr>
                        <td>${p.playerName || p.playerUuid}</td>
                        <td>${getPunishmentTypeText(p.type)}</td>
                        <td>${p.reason}</td>
                        <td>${formatDate(p.createdAt)}</td>
                        <td><span class="punishment-status status-${p.status}">${getStatusText(p.status)}</span></td>
                    </tr>
                `).join('')}
            </tbody>
        </table>
    `;

    container.innerHTML = table;
}

// 玩家搜索
let currentPlayer = null;

document.getElementById('searchBtn').addEventListener('click', async () => {
    const query = document.getElementById('playerSearch').value.trim();
    if (!query) return;

    const data = await apiRequest(`/api/players/search/${encodeURIComponent(query)}`);

    if (data && data.player) {
        currentPlayer = data.player;
        displayPlayerInfo(data.player);
        document.getElementById('playerResult').style.display = 'block';
    } else {
        alert('未找到该玩家');
        document.getElementById('playerResult').style.display = 'none';
    }
});

function displayPlayerInfo(player) {
    const html = `
        <div class="player-card">
            <h3>${player.playerName || '未知玩家'}</h3>
            <p><strong>UUID:</strong> ${player.uuid}</p>
            <p><strong>最后登录IP:</strong> ${player.lastIp || '未知'}</p>
            <p><strong>首次登录:</strong> ${formatDate(player.firstJoin)}</p>
            <p><strong>最后登录:</strong> ${formatDate(player.lastJoin)}</p>
        </div>
    `;

    document.getElementById('playerInfo').innerHTML = html;
}

// 查看历史
document.getElementById('viewHistoryBtn').addEventListener('click', async () => {
    if (!currentPlayer) return;

    const data = await apiRequest(`/api/punishments/history/${currentPlayer.playerName || currentPlayer.uuid}`);

    if (data && data.punishments) {
        displayPunishmentHistory(data.punishments);
        document.getElementById('playerHistory').style.display = 'block';
    }
});

function displayPunishmentHistory(punishments) {
    const container = document.getElementById('historyList');

    if (punishments.length === 0) {
        container.innerHTML = '<p class="loading">该玩家没有处罚记录</p>';
        return;
    }

    const html = punishments.map(p => `
        <div class="punishment-item ${p.status}">
            <div class="punishment-header">
                <span class="punishment-type">${getPunishmentTypeText(p.type)}</span>
                <span class="punishment-status status-${p.status}">${getStatusText(p.status)}</span>
            </div>
            <div class="punishment-details">
                <p><strong>原因:</strong> ${p.reason}</p>
                <p><strong>执行者:</strong> ${p.issuerName || p.issuerUuid}</p>
                <p><strong>时间:</strong> ${formatDate(p.createdAt)}</p>
                ${p.duration ? `<p><strong>时长:</strong> ${formatDuration(p.duration)}</p>` : '<p><strong>时长:</strong> 永久</p>'}
                ${p.expiresAt ? `<p><strong>到期:</strong> ${formatDate(p.expiresAt)}</p>` : ''}
            </div>
        </div>
    `).join('');

    container.innerHTML = html;
}

// 封禁玩家
document.getElementById('banPlayerBtn').addEventListener('click', () => {
    if (!currentPlayer) return;
    openModal('banModal');
});

document.getElementById('banForm').addEventListener('submit', async (e) => {
    e.preventDefault();

    if (!currentPlayer) return;

    const reason = document.getElementById('banReason').value;
    const duration = document.getElementById('banDuration').value;
    const silent = document.getElementById('banSilent').checked;

    const payload = {
        player: currentPlayer.playerName || currentPlayer.uuid,
        reason: reason,
        silent: silent
    };

    if (duration) {
        payload.duration = parseInt(duration);
    }

    const result = await apiRequest('/api/punishments/ban', {
        method: 'POST',
        body: JSON.stringify(payload)
    });

    if (result && result.success) {
        alert('封禁成功');
        closeModal('banModal');
        document.getElementById('banForm').reset();
    } else {
        alert('封禁失败: ' + (result?.error || '未知错误'));
    }
});

// 解封玩家
document.getElementById('unbanPlayerBtn').addEventListener('click', async () => {
    if (!currentPlayer) return;

    if (!confirm('确定要解封该玩家吗？')) return;

    const result = await apiRequest('/api/punishments/unban', {
        method: 'POST',
        body: JSON.stringify({
            player: currentPlayer.playerName || currentPlayer.uuid
        })
    });

    if (result && result.success) {
        alert('解封成功');
    } else {
        alert('解封失败: ' + (result?.error || '未知错误'));
    }
});

// 撤销所有处罚
document.getElementById('revokeAllBtn').addEventListener('click', async () => {
    if (!currentPlayer) return;

    const reason = prompt('请输入撤销原因:');
    if (!reason) return;

    const result = await apiRequest('/api/punishments/revoke', {
        method: 'POST',
        body: JSON.stringify({
            player: currentPlayer.playerName || currentPlayer.uuid,
            reason: reason
        })
    });

    if (result && result.success) {
        alert('撤销成功');
    } else {
        alert('撤销失败: ' + (result?.error || '未知错误'));
    }
});

// 模态框控制
function openModal(modalId) {
    document.getElementById(modalId).classList.add('active');
}

function closeModal(modalId) {
    document.getElementById(modalId).classList.remove('active');
}

// 工具函数
function getPunishmentTypeText(type) {
    const types = {
        'BAN': '封禁',
        'TEMP_BAN': '临时封禁',
        'KICK': '踢出',
        'MUTE': '禁言',
        'TEMP_MUTE': '临时禁言',
        'WARNING': '警告'
    };
    return types[type] || type;
}

function getStatusText(status) {
    const statuses = {
        'ACTIVE': '生效中',
        'EXPIRED': '已过期',
        'REVOKED': '已撤销'
    };
    return statuses[status] || status;
}

function formatDate(timestamp) {
    if (!timestamp) return '未知';
    const date = new Date(timestamp);
    return date.toLocaleString('zh-CN');
}

function formatDuration(ms) {
    const seconds = Math.floor(ms / 1000);
    const minutes = Math.floor(seconds / 60);
    const hours = Math.floor(minutes / 60);
    const days = Math.floor(hours / 24);

    if (days > 0) return `${days}天`;
    if (hours > 0) return `${hours}小时`;
    if (minutes > 0) return `${minutes}分钟`;
    return `${seconds}秒`;
}

// 处罚记录页面
let allPunishments = [];

async function loadAllPunishments() {
    const container = document.getElementById('allPunishmentsList');
    container.innerHTML = '<p class="loading">加载中...</p>';

    // 由于后端没有获取所有处罚的API，我们使用仪表盘的数据作为示例
    const recentData = await apiRequest('/api/stats/recent');

    if (recentData && recentData.recent) {
        allPunishments = recentData.recent;
        displayAllPunishments(allPunishments);
    } else {
        container.innerHTML = '<p class="loading">暂无处罚记录</p>';
    }
}

function displayAllPunishments(punishments) {
    const container = document.getElementById('allPunishmentsList');

    if (punishments.length === 0) {
        container.innerHTML = '<p class="loading">暂无处罚记录</p>';
        return;
    }

    const table = `
        <table>
            <thead>
                <tr>
                    <th>玩家</th>
                    <th>类型</th>
                    <th>原因</th>
                    <th>执行者</th>
                    <th>时间</th>
                    <th>状态</th>
                </tr>
            </thead>
            <tbody>
                ${punishments.map(p => `
                    <tr>
                        <td>${p.playerName || p.playerUuid || '未知'}</td>
                        <td>${getPunishmentTypeText(p.type)}</td>
                        <td>${p.reason || '无'}</td>
                        <td>${p.issuerName || p.issuerUuid || '系统'}</td>
                        <td>${formatDate(p.createdAt)}</td>
                        <td><span class="punishment-status status-${p.status || 'active'}">${getStatusText(p.status || 'ACTIVE')}</span></td>
                    </tr>
                `).join('')}
            </tbody>
        </table>
    `;

    container.innerHTML = table;
}

// 处罚记录搜索和筛选
document.getElementById('searchPunishmentsBtn').addEventListener('click', () => {
    const searchQuery = document.getElementById('punishmentSearch').value.trim().toLowerCase();
    const typeFilter = document.getElementById('punishmentTypeFilter').value;
    const statusFilter = document.getElementById('punishmentStatusFilter').value;

    let filtered = allPunishments;

    // 按玩家名称或UUID搜索
    if (searchQuery) {
        filtered = filtered.filter(p => {
            const playerName = (p.playerName || '').toLowerCase();
            const playerUuid = (p.playerUuid || '').toLowerCase();
            return playerName.includes(searchQuery) || playerUuid.includes(searchQuery);
        });
    }

    // 按类型筛选
    if (typeFilter) {
        filtered = filtered.filter(p => p.type === typeFilter);
    }

    // 按状态筛选
    if (statusFilter) {
        filtered = filtered.filter(p => (p.status || 'active').toLowerCase() === statusFilter);
    }

    displayAllPunishments(filtered);
});

// 初始加载
loadDashboard();
