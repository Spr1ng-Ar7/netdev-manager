let isAdmin = false;

authGuard('users').then(init);

function init() {
    isAdmin = currentUser.role === 'ADMIN';
    if (!isAdmin) {
        document.querySelector('.content').innerHTML = '<div class="card"><div class="empty">仅管理员可管理用户</div></div>';
        return;
    }
    document.getElementById('btn-add').style.display = '';
    document.getElementById('f-keyword').addEventListener('keydown', e => e.key === 'Enter' && loadList());
    loadList();
}

async function loadList() {
    const kw = document.getElementById('f-keyword').value.trim();
    let users;
    try {
        users = await request('/api/users' + (kw ? '?keyword=' + encodeURIComponent(kw) : ''));
    } catch (e) { toast(e.message, 'error'); return; }

    const tbody = document.getElementById('tbody');
    if (!users.length) {
        tbody.innerHTML = '<tr><td colspan="8"><div class="empty">暂无用户</div></td></tr>';
        return;
    }
    tbody.innerHTML = users.map(u => `
        <tr>
            <td>${esc(u.username)}</td>
            <td>${esc(u.name)}</td>
            <td>${u.role === 'ADMIN' ? '<span class="badge badge-primary">管理员</span>' : '<span class="badge badge-gray">普通用户</span>'}</td>
            <td>${esc(u.phone || '-')}</td>
            <td>${esc(u.email || '-')}</td>
            <td>${u.enabled ? '<span class="badge badge-green">启用</span>' : '<span class="badge badge-clay">禁用</span>'}</td>
            <td>${fmtDate(u.createdAt)}</td>
            <td class="ops">
                <button class="btn btn-sm" onclick='openForm(${JSON.stringify(u).replace(/'/g, "&#39;")})'>编辑</button>
                ${u.id !== currentUser.id ? `<button class="btn btn-pink btn-sm" onclick="deleteUser(${u.id}, '${esc(u.username)}')">删除</button>` : ''}
            </td>
        </tr>`).join('');
}

function openForm(user) {
    const isEdit = !!user;
    openModal(isEdit ? `编辑用户 ${user.username}` : '新增用户', `
        <div class="form-grid">
            <div class="form-item"><label>用户名<span class="req">*</span></label>
                <input class="input" id="u-username" value="${esc(user ? user.username : '')}" ${isEdit ? 'disabled' : ''}></div>
            <div class="form-item"><label>姓名<span class="req">*</span></label>
                <input class="input" id="u-name" value="${esc(user ? user.name : '')}"></div>
            <div class="form-item"><label>${isEdit ? '重置密码（留空不修改）' : '密码（至少 6 位）'}${isEdit ? '' : '<span class="req">*</span>'}</label>
                <input class="input" id="u-password" type="password" placeholder="${isEdit ? '留空则不修改' : ''}"></div>
            <div class="form-item"><label>角色</label>
                <select class="select" id="u-role">
                    <option value="USER" ${user && user.role === 'USER' ? 'selected' : ''}>普通用户</option>
                    <option value="ADMIN" ${user && user.role === 'ADMIN' ? 'selected' : ''}>管理员</option>
                </select></div>
            <div class="form-item"><label>电话</label>
                <input class="input" id="u-phone" value="${esc(user ? user.phone || '' : '')}"></div>
            <div class="form-item"><label>邮箱</label>
                <input class="input" id="u-email" value="${esc(user ? user.email || '' : '')}"></div>
            <div class="form-item"><label>账号状态</label>
                <select class="select" id="u-enabled">
                    <option value="true" ${!user || user.enabled ? 'selected' : ''}>启用</option>
                    <option value="false" ${user && !user.enabled ? 'selected' : ''}>禁用</option>
                </select></div>
        </div>`,
        `<button class="btn btn-plain" onclick="closeModal()">取消</button>
         <button class="btn btn-green" id="u-save">保存</button>`);

    document.getElementById('u-save').onclick = async () => {
        const body = {
            username: document.getElementById('u-username').value.trim(),
            name: document.getElementById('u-name').value.trim(),
            password: document.getElementById('u-password').value || null,
            role: document.getElementById('u-role').value,
            phone: document.getElementById('u-phone').value.trim() || null,
            email: document.getElementById('u-email').value.trim() || null,
            enabled: document.getElementById('u-enabled').value === 'true',
        };
        if (!body.name) { toast('请填写姓名', 'error'); return; }
        if (!isEdit) {
            if (!body.username) { toast('请填写用户名', 'error'); return; }
            if (!body.password) { toast('请设置密码', 'error'); return; }
        }
        try {
            if (isEdit) {
                await request('/api/users/' + user.id, { method: 'PUT', body });
            } else {
                await request('/api/users', { method: 'POST', body });
            }
            toast('保存成功', 'success');
            closeModal();
            loadList();
        } catch (e) { toast(e.message, 'error'); }
    };
}

function deleteUser(id, username) {
    confirmDialog(`确定删除用户「${username}」？`, async () => {
        try {
            await request('/api/users/' + id, { method: 'DELETE' });
            toast('删除成功', 'success');
            loadList();
        } catch (e) { toast(e.message, 'error'); }
    });
}
