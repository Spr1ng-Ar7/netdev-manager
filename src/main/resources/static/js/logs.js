let curPage = 1;
const pageSize = 15;

authGuard('logs').then(init);

function init() {
    if (currentUser.role !== 'ADMIN') {
        document.querySelector('.content').innerHTML = '<div class="card"><div class="empty">仅管理员可查看操作日志</div></div>';
        return;
    }
    document.getElementById('f-keyword').addEventListener('keydown', e => e.key === 'Enter' && loadList(1));
    loadList(1);
}

async function loadList(page = 1) {
    curPage = page;
    const params = new URLSearchParams({ page, size: pageSize });
    const module = document.getElementById('f-module').value;
    if (module) params.set('module', module);
    const kw = document.getElementById('f-keyword').value.trim();
    if (kw) params.set('keyword', kw);
    let data;
    try {
        data = await request('/api/logs?' + params.toString());
    } catch (e) { toast(e.message, 'error'); return; }

    const tbody = document.getElementById('tbody');
    if (!data.list.length) {
        tbody.innerHTML = '<tr><td colspan="6"><div class="empty">暂无日志</div></td></tr>';
    } else {
        tbody.innerHTML = data.list.map(l => `
            <tr>
                <td style="white-space:nowrap">${fmtTime(l.createdAt)}</td>
                <td>${esc(l.username || '-')}</td>
                <td>${esc(l.module)}</td>
                <td>${esc(l.action)}</td>
                <td title="${esc(l.detail || '')}">${esc((l.detail || '-').substring(0, 40))}${(l.detail || '').length > 40 ? '…' : ''}</td>
                <td>${esc(l.ip || '-')}</td>
            </tr>`).join('');
    }
    renderPagination('pager', page, data.total, pageSize, loadList);
}

function resetFilters() {
    document.getElementById('f-module').value = '';
    document.getElementById('f-keyword').value = '';
    loadList(1);
}
