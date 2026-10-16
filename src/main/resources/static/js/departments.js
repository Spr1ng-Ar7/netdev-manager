let isAdmin = false;

authGuard('departments').then(init);

function init() {
    isAdmin = currentUser.role === 'ADMIN';
    if (isAdmin) {
        document.getElementById('btn-add-dept').style.display = '';
        document.getElementById('btn-add-loc').style.display = '';
    }
    loadAll();
}

async function loadAll() {
    try {
        const [depts, locs] = await Promise.all([
            request('/api/departments'), request('/api/locations')
        ]);
        renderTable('dept-tbody', depts, 'departments');
        renderTable('loc-tbody', locs, 'locations');
    } catch (e) { toast(e.message, 'error'); }
}

function renderTable(tbodyId, list, api) {
    const tbody = document.getElementById(tbodyId);
    if (!list.length) {
        tbody.innerHTML = '<tr><td colspan="4"><div class="empty">暂无数据</div></td></tr>';
        return;
    }
    tbody.innerHTML = list.map(x => `
        <tr>
            <td>${esc(x.name)}</td>
            <td>${esc(x.remark || '-')}</td>
            <td>${fmtDate(x.createdAt)}</td>
            <td class="ops">
                ${isAdmin ? `
                <button class="btn btn-sm" onclick="openForm('${api}', ${x.id})">编辑</button>
                <button class="btn btn-pink btn-sm" onclick="deleteItem('${api}', ${x.id}, '${esc(x.name)}')">删除</button>` : '-'}
            </td>
        </tr>`).join('');
}

async function openForm(api, id) {
    let item = null;
    if (id) {
        try {
            const list = await request('/api/' + api);
            item = list.find(x => x.id === id);
        } catch (e) { toast(e.message, 'error'); return; }
    }
    const title = api === 'departments' ? '部门' : '位置';
    openModal(id ? `编辑${title}` : `新增${title}`, `
        <div class="form-item" style="margin-bottom:14px"><label>名称<span class="req">*</span></label>
            <input class="input" id="n-name" value="${esc(item ? item.name : '')}" style="width:100%"></div>
        <div class="form-item"><label>备注</label>
            <textarea class="textarea" id="n-remark">${esc(item ? item.remark || '' : '')}</textarea></div>`,
        `<button class="btn btn-plain" onclick="closeModal()">取消</button>
         <button class="btn btn-green" id="n-save">保存</button>`);
    document.getElementById('n-save').onclick = async () => {
        const name = document.getElementById('n-name').value.trim();
        if (!name) { toast('请填写名称', 'error'); return; }
        const body = { name, remark: document.getElementById('n-remark').value.trim() || null };
        try {
            if (id) {
                await request(`/api/${api}/${id}`, { method: 'PUT', body });
            } else {
                await request('/api/' + api, { method: 'POST', body });
            }
            toast('保存成功', 'success');
            closeModal();
            loadAll();
        } catch (e) { toast(e.message, 'error'); }
    };
}

function deleteItem(api, id, name) {
    confirmDialog(`确定删除「${name}」？若其下仍有设备将无法删除。`, async () => {
        try {
            await request(`/api/${api}/${id}`, { method: 'DELETE' });
            toast('删除成功', 'success');
            loadAll();
        } catch (e) { toast(e.message, 'error'); }
    });
}
