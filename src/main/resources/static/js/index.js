const MORANDI = ['#8C9BA5', '#A9B8A1', '#C9A9A6', '#AFA7BA', '#C7B9A5', '#C08E8E', '#768794', '#93A58A', '#B78F8C'];

authGuard('index').then(loadStats);

async function loadStats() {
    let stats;
    try {
        stats = await request('/api/dashboard/stats');
    } catch (e) {
        toast(e.message, 'error');
        return;
    }
    renderStatCards(stats);
    if (typeof echarts === 'undefined') {
        document.querySelectorAll('.chart-box').forEach(el =>
            el.innerHTML = '<div class="empty">图表组件加载失败（无法访问 CDN）</div>');
        return;
    }
    renderPie('chart-type', stats.deviceByType || []);
    renderPie('chart-status', (stats.deviceByStatus || []).map(x => ({
        name: (DEVICE_STATUS[x.name] || {}).label || x.name, value: x.value
    })));
    renderPie('chart-dept', stats.deviceByDepartment || []);
    renderTrend(stats.purchaseTrend || []);
}

function renderStatCards(s) {
    const cards = [
        { icon: '🖥️', bg: 'var(--primary)', num: s.deviceTotal, label: '设备总数' },
        { icon: '✅', bg: 'var(--green)', num: s.deviceInUse, label: '在用设备' },
        { icon: '⚠️', bg: 'var(--clay)', num: s.deviceFault, label: '故障/维修中' },
        { icon: '🔧', bg: 'var(--lavender)', num: s.maintenanceOpen, label: '待处理维保' },
        { icon: '📦', bg: 'var(--sand)', num: s.loanOut, label: '借出中设备' },
        { icon: '⏰', bg: 'var(--pink)', num: s.loanOverdue, label: '借出已超期' },
        { icon: '📋', bg: 'var(--green-dark)', num: s.inspectionTotal, label: '巡检记录' },
        { icon: '👥', bg: 'var(--primary-dark)', num: s.userTotal, label: '系统用户' },
    ];
    document.getElementById('stat-cards').innerHTML = cards.map(c => `
        <div class="stat-card">
            <div class="stat-icon" style="background:${c.bg}">${c.icon}</div>
            <div>
                <div class="stat-num">${c.num}</div>
                <div class="stat-label">${c.label}</div>
            </div>
        </div>`).join('');
}

function renderPie(domId, data) {
    const chart = echarts.init(document.getElementById(domId));
    chart.setOption({
        color: MORANDI,
        tooltip: { trigger: 'item', formatter: '{b}：{c} 台（{d}%）' },
        legend: { bottom: 0, textStyle: { color: '#8A8782', fontSize: 12 } },
        series: [{
            type: 'pie',
            radius: ['38%', '62%'],
            center: ['50%', '44%'],
            itemStyle: { borderRadius: 6, borderColor: '#fff', borderWidth: 2 },
            label: { color: '#4A4A48', fontSize: 12 },
            data: data.length ? data : [{ name: '暂无数据', value: 1, itemStyle: { color: '#DDD8D2' } }]
        }]
    });
    window.addEventListener('resize', () => chart.resize());
}

function renderTrend(data) {
    const chart = echarts.init(document.getElementById('chart-trend'));
    chart.setOption({
        color: MORANDI,
        tooltip: { trigger: 'axis' },
        grid: { left: 40, right: 20, top: 30, bottom: 40 },
        xAxis: {
            type: 'category',
            data: data.map(d => d.month),
            axisLabel: { color: '#8A8782', fontSize: 11, rotate: 30 },
            axisLine: { lineStyle: { color: '#DDD8D2' } }
        },
        yAxis: {
            type: 'value',
            minInterval: 1,
            axisLabel: { color: '#8A8782' },
            splitLine: { lineStyle: { color: '#EFEBE7' } }
        },
        series: [{
            type: 'bar',
            data: data.map(d => d.count),
            barWidth: 18,
            itemStyle: { borderRadius: [4, 4, 0, 0], color: '#A9B8A1' }
        }]
    });
    window.addEventListener('resize', () => chart.resize());
}
