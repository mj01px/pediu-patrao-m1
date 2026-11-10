// Dashboard administrativo: consome a /api/dashboard (RF06) e mantem os numeros atualizados (RF07).
(() => {
    'use strict';

    const API = '/api/dashboard';
    const INTERVALO_MS = 60_000;
    const STATUS_ROTULO = {};

    const $ = id => document.getElementById(id);
    const estado = {
        agrupamento: 'DIA',
        medida: 'faturamento',
        ordem: 'QUANTIDADE',
        serie: null,
        grafico: null,
        controle: null,   // AbortController da carga em andamento
        timer: null
    };

    const inteiro = n => Number(n ?? 0).toLocaleString('pt-BR');
    const pct = n => n == null ? '–' : Number(n).toLocaleString('pt-BR', { maximumFractionDigits: 1 }) + '%';
    const hojeIso = () => {
        const d = new Date();
        return new Date(d.getTime() - d.getTimezoneOffset() * 60000).toISOString().slice(0, 10);
    };
    const somarDias = (iso, dias) => {
        const d = new Date(iso + 'T12:00:00');
        d.setDate(d.getDate() + dias);
        return d.toISOString().slice(0, 10);
    };
    const dataBr = iso => iso.split('-').reverse().join('/');

    // ---------- chamadas a API ----------

    class ErroApi extends Error {
        constructor(mensagem, status) {
            super(mensagem);
            this.status = status;
        }
    }

    async function buscar(caminho, params, sinal) {
        const url = API + caminho + (params ? '?' + params : '');
        let resposta;
        try {
            resposta = await fetch(url, { headers: { Accept: 'application/json' }, signal: sinal });
        } catch (e) {
            if (e.name === 'AbortError') throw e;
            throw new ErroApi('Sem conexão com o servidor. Verifique a rede e tente de novo.', 0);
        }
        if (!resposta.ok) {
            throw new ErroApi(await PP.lerProblema(resposta), resposta.status);
        }
        return resposta.json();
    }

    // ---------- filtros (RF05) ----------

    function lerFiltros() {
        const p = new URLSearchParams();
        ['fInicio', 'fFim', 'fTipo', 'fStatus'].forEach(id => {
            const campo = $(id);
            if (campo.value) p.set(campo.name, campo.value);
        });
        return p;
    }

    function validarFiltros() {
        const inicio = $('fInicio').value;
        const fim = $('fFim').value;
        let erro = '';
        if (!inicio || !fim) erro = 'Informe as duas datas do período.';
        else if (inicio > fim) erro = 'A data inicial não pode ser depois da final.';
        $('erroFiltro').textContent = erro;
        $('erroFiltro').hidden = !erro;
        return !erro;
    }

    function marcarPeriodoRapido() {
        const inicio = $('fInicio').value;
        const fim = $('fFim').value;
        document.querySelectorAll('[data-dias]').forEach(botao => {
            const dias = Number(botao.dataset.dias);
            botao.classList.toggle('ativo', fim === hojeIso() && inicio === somarDias(fim, 1 - dias));
        });
    }

    function sincronizarUrl() {
        const p = lerFiltros();
        history.replaceState(null, '', location.pathname + (p.toString() ? '?' + p : ''));
    }

    async function prepararFiltros() {
        const opcoes = await buscar('/filtros');
        opcoes.tipos.forEach(tipo => $('fTipo').add(new Option(tipo, tipo)));
        opcoes.status.forEach(s => {
            STATUS_ROTULO[s.valor] = s.rotulo;
            $('fStatus').add(new Option(s.rotulo, s.valor));
        });
        const daUrl = new URLSearchParams(location.search);
        $('fInicio').value = daUrl.get('inicio') || opcoes.inicioPadrao;
        $('fFim').value = daUrl.get('fim') || opcoes.fimPadrao;
        $('fTipo').value = daUrl.get('tipo') || '';
        $('fStatus').value = daUrl.get('status') || '';
        estado.padrao = { inicio: opcoes.inicioPadrao, fim: opcoes.fimPadrao };
        marcarPeriodoRapido();
    }

    // ---------- carga (RF06 + RF07) ----------

    async function carregar() {
        if (!validarFiltros()) return;
        estado.controle?.abort(); // uma carga nova cancela a anterior: so a resposta mais recente aparece
        const controle = new AbortController();
        estado.controle = controle;

        const filtro = lerFiltros();
        const comAgrupamento = new URLSearchParams(filtro);
        comAgrupamento.set('agrupamento', estado.agrupamento);
        const comOrdem = new URLSearchParams(filtro);
        comOrdem.set('ordenarPor', estado.ordem);
        comOrdem.set('limite', '10');

        ocupado(true);
        try {
            const [resumo, vendas, categorias, status, ranking] = await Promise.all([
                buscar('/resumo', filtro, controle.signal),
                buscar('/vendas', comAgrupamento, controle.signal),
                buscar('/categorias', filtro, controle.signal),
                buscar('/status', filtro, controle.signal),
                buscar('/produtos/mais-vendidos', comOrdem, controle.signal)
            ]);
            desenharResumo(resumo);
            estado.serie = vendas;
            desenharVendas();
            desenharCategorias(categorias);
            desenharStatus(status);
            desenharRanking(ranking);
            $('falha').hidden = true;
            $('atualizadoEm').textContent = 'Atualizado às ' + resumo.geradoEm.slice(11, 19);
        } catch (e) {
            if (e.name === 'AbortError') return;
            tratarFalha(e);
        } finally {
            if (estado.controle === controle) ocupado(false);
        }
    }

    function tratarFalha(erro) {
        if (erro.status === 401) {
            // Sessao expirou ou foi encerrada (perfil alterado): volta para o login
            location.href = '/login?expirada';
            return;
        }
        // Os dados anteriores ficam na tela; o aviso diz que podem estar desatualizados
        $('falhaTexto').textContent = erro.message + (estado.serie ? ' Os números exibidos podem estar desatualizados.' : '');
        $('falha').hidden = false;
        $('atualizadoEm').textContent = 'Falha ao atualizar';
    }

    function ocupado(sim) {
        $('dashboard').classList.toggle('carregando', sim);
        $('kpis').setAttribute('aria-busy', String(sim));
        $('atualizar').disabled = sim;
    }

    function agendar() {
        clearInterval(estado.timer);
        if ($('autoAtualizar').checked) {
            // Aba escondida nao consulta o servidor a toa
            estado.timer = setInterval(() => document.visibilityState === 'visible' && carregar(), INTERVALO_MS);
        }
    }

    // ---------- desenho ----------

    function variacaoHtml(valor, inverso = false) {
        if (valor == null) return '<span class="muted">sem base no período anterior</span>';
        const n = Number(valor);
        const bom = inverso ? n < 0 : n > 0;
        const classe = n === 0 ? 'neutra' : bom ? 'sobe' : 'desce';
        const seta = n > 0 ? '▲' : n < 0 ? '▼' : '■';
        return `<span class="variacao ${classe}">${seta} ${pct(Math.abs(n))}</span> <span class="muted">vs. período anterior</span>`;
    }

    function kpi(nome, valor, nota) {
        const card = document.querySelector(`[data-kpi="${nome}"]`);
        card.querySelector('.kpi-valor').textContent = valor;
        card.querySelector('.kpi-nota').innerHTML = nota;
    }

    function desenharResumo({ indicadores: i, periodoAnterior: a }) {
        kpi('faturamento', PP.moeda(i.faturamento), variacaoHtml(a.variacaoFaturamento));
        kpi('pedidosValidos', inteiro(i.pedidosValidos), variacaoHtml(a.variacaoPedidos));
        kpi('ticketMedio', PP.moeda(i.ticketMedio), variacaoHtml(a.variacaoTicketMedio));
        kpi('itensVendidos', inteiro(i.itensVendidos),
            `<span class="muted">${inteiro(i.emAndamento)} pedido(s) ainda em andamento</span>`);
        kpi('cancelados', inteiro(i.cancelados),
            `<span class="muted">${pct(i.taxaCancelamento)} dos ${inteiro(i.pedidos)} pedidos</span>`);
        kpi('descontoConcedido', PP.moeda(i.descontoConcedido),
            `<span class="muted">em ${inteiro(i.pedidosComDesconto)} pedido(s)</span>`);
    }

    function desenharVendas() {
        const serie = estado.serie;
        if (!serie) return;
        const ehFaturamento = estado.medida === 'faturamento';
        const f = serie.filtro;
        const periodo = f.dias === 1 ? dataBr(f.inicio) : `${dataBr(f.inicio)} a ${dataBr(f.fim)}`;
        $('legendaVendas').textContent = `${periodo} · ` + (ehFaturamento
            ? 'faturamento líquido, sem pedidos cancelados.' : 'pedidos válidos, sem cancelados.');
        const rotulos = serie.pontos.map(p => p.rotulo);
        const valores = serie.pontos.map(p => ehFaturamento ? Number(p.faturamento) : p.pedidos);
        const vazio = serie.pontos.every(p => p.pedidos === 0);
        $('vendasVazio').hidden = !vazio;

        // Tabela equivalente ao grafico (acessibilidade e conferencia dos numeros)
        $('tabelaVendas').innerHTML = '<thead><tr><th>Período</th><th class="direita">Pedidos</th>'
            + '<th class="direita">Faturamento</th><th class="direita">Ticket médio</th></tr></thead><tbody>'
            + serie.pontos.map(p => `<tr><td>${PP.texto(p.rotulo)}</td><td class="direita mono">${inteiro(p.pedidos)}</td>`
                + `<td class="direita mono">${PP.moeda(p.faturamento)}</td><td class="direita mono">${PP.moeda(p.ticketMedio)}</td></tr>`).join('')
            + '</tbody>';

        if (typeof Chart === 'undefined') {
            // Biblioteca do grafico nao carregou (rede bloqueada): a tabela fica aberta no lugar
            document.querySelector('.tabela-dados').open = true;
            document.querySelector('.grafico').hidden = true;
            return;
        }
        const formatar = v => ehFaturamento ? PP.moeda(v) : inteiro(v);
        const dados = {
            labels: rotulos,
            datasets: [{
                label: ehFaturamento ? 'Faturamento' : 'Pedidos',
                data: valores,
                backgroundColor: '#4A4440',
                hoverBackgroundColor: '#D7341C',
                borderRadius: 4,
                borderSkipped: 'start',
                maxBarThickness: 36
            }]
        };
        if (estado.grafico) {
            estado.grafico.data = dados;
            estado.grafico.options.scales.y.ticks.callback = formatar;
            estado.grafico.options.plugins.tooltip.callbacks.label = c => ' ' + formatar(c.parsed.y);
            estado.grafico.update();
            return;
        }
        estado.grafico = new Chart($('graficoVendas'), {
            type: 'bar',
            data: dados,
            options: {
                maintainAspectRatio: false,
                animation: { duration: 250 },
                interaction: { mode: 'index', intersect: false },
                plugins: {
                    legend: { display: false },
                    tooltip: {
                        backgroundColor: '#1C1917', padding: 10, displayColors: false,
                        titleFont: { family: 'Figtree', weight: '600' }, bodyFont: { family: 'JetBrains Mono' },
                        callbacks: { label: c => ' ' + formatar(c.parsed.y) }
                    }
                },
                scales: {
                    x: { grid: { display: false }, ticks: { color: '#6B645E', maxRotation: 0, autoSkip: true, font: { family: 'Figtree' } } },
                    y: {
                        beginAtZero: true, border: { display: false }, grid: { color: '#F0EEEB' },
                        ticks: { color: '#6B645E', callback: formatar, font: { family: 'JetBrains Mono', size: 11 }, precision: 0 }
                    }
                }
            }
        });
    }

    function barra(rotulo, valor, percentual, classe = '') {
        return `<li>
            <div class="barra-linha"><span class="barra-rotulo">${rotulo}</span><span class="mono">${valor}</span></div>
            <div class="barra-trilho"><span class="barra-cheia ${classe}" style="width:${Math.min(100, Number(percentual))}%"></span></div>
        </li>`;
    }

    function desenharStatus({ total, etapas, tempos }) {
        $('totalStatus').textContent = `${inteiro(total)} pedido(s) no período`;
        $('listaStatus').innerHTML = total === 0
            ? '<li class="muted">Nenhum pedido no período.</li>'
            : etapas.filter(e => e.quantidade > 0).map(e => barra(
                `<span class="selo selo-p s-${e.status}"><span class="ponto"></span>${PP.texto(e.rotulo)}</span>`,
                `${inteiro(e.quantidade)} · ${pct(e.percentual)}`, e.percentual, 's-barra-' + e.status)).join('');
        const min = m => m == null ? '–' : m >= 60 ? `${Math.floor(m / 60)} h ${m % 60} min` : `${m} min`;
        $('tempos').innerHTML = `
            <div><span class="sobretitulo">Até ficar pronto</span><span class="mono">${min(tempos.preparoMedioMin)}</span></div>
            <div><span class="sobretitulo">Até sair</span><span class="mono">${min(tempos.saidaMediaMin)}</span></div>
            <div><span class="sobretitulo">Até finalizar</span><span class="mono">${min(tempos.conclusaoMediaMin)}</span></div>`;
    }

    function desenharCategorias({ categorias }) {
        $('listaCategorias').innerHTML = categorias.length === 0
            ? '<li class="muted">Nenhuma venda no período.</li>'
            : categorias.map(c => barra(
                `<strong>${PP.texto(c.tipo)}</strong> <span class="muted">${inteiro(c.quantidade)} un.</span>`,
                `${PP.moeda(c.faturamento)} · ${pct(c.participacao)}`, c.participacao)).join('');
    }

    function desenharRanking({ produtos }) {
        $('ranking').innerHTML = produtos.length === 0
            ? '<tr><td colspan="6" class="vazio">Nenhum produto vendido no período.</td></tr>'
            : produtos.map(p => `<tr>
                <td class="mono">${p.posicao}</td>
                <td><strong>${PP.texto(p.nome)}</strong></td>
                <td>${PP.texto(p.tipo)}</td>
                <td class="direita mono">${inteiro(p.quantidade)}</td>
                <td class="direita mono">${PP.moeda(p.faturamento)}</td>
                <td><div class="participacao"><div class="barra-trilho"><span class="barra-cheia" style="width:${Math.min(100, Number(p.participacao))}%"></span></div><span class="mono">${pct(p.participacao)}</span></div></td>
            </tr>`).join('');
    }

    function segmento(grupo, atributo, aoEscolher) {
        $(grupo).addEventListener('click', evento => {
            const botao = evento.target.closest('button');
            if (!botao) return;
            $(grupo).querySelectorAll('button').forEach(b => b.classList.toggle('ativo', b === botao));
            aoEscolher(botao.dataset[atributo]);
        });
    }

    // ---------- inicio ----------

    document.addEventListener('DOMContentLoaded', async () => {
        $('filtros').addEventListener('submit', evento => {
            evento.preventDefault();
            marcarPeriodoRapido();
            sincronizarUrl();
            carregar();
        });
        document.querySelectorAll('[data-dias]').forEach(botao => botao.addEventListener('click', () => {
            const fim = hojeIso();
            $('fFim').value = fim;
            $('fInicio').value = somarDias(fim, 1 - Number(botao.dataset.dias));
            $('filtros').requestSubmit();
        }));
        $('limpar').addEventListener('click', () => {
            $('fInicio').value = estado.padrao?.inicio || '';
            $('fFim').value = estado.padrao?.fim || '';
            $('fTipo').value = '';
            $('fStatus').value = '';
            $('filtros').requestSubmit();
        });
        $('atualizar').addEventListener('click', carregar);
        $('tentarDeNovo').addEventListener('click', carregar);
        $('autoAtualizar').addEventListener('change', agendar);
        document.addEventListener('visibilitychange', () => document.visibilityState === 'visible'
            && $('autoAtualizar').checked && carregar());

        segmento('agrupamento', 'agrupamento', valor => { estado.agrupamento = valor; carregar(); });
        segmento('ordem', 'ordem', valor => { estado.ordem = valor; carregar(); });
        segmento('medida', 'medida', valor => {
            estado.medida = valor;
            $('tituloVendas').textContent = valor === 'faturamento' ? 'Vendas no período' : 'Pedidos no período';
            desenharVendas(); // so troca a medida exibida, sem nova consulta
        });

        try {
            await prepararFiltros();
        } catch (e) {
            tratarFalha(e);
            return;
        }
        await carregar();
        agendar();
    });
})();
