// Menu lateral em telas pequenas (o script fica no <head>, entao espera o HTML)
document.addEventListener('DOMContentLoaded', () => {
    const menu = document.getElementById('menu');
    const botao = document.getElementById('abrirMenu');
    if (menu && botao) {
        botao.addEventListener('click', evento => {
            evento.stopPropagation();
            botao.setAttribute('aria-expanded', menu.classList.toggle('aberto'));
        });
        // Clicar fora do menu aberto fecha o menu
        document.addEventListener('click', evento => {
            if (menu.classList.contains('aberto') && !menu.contains(evento.target)) {
                menu.classList.remove('aberto');
                botao.setAttribute('aria-expanded', 'false');
            }
        });
    }

    // Clicar no fundo escuro de um <dialog class="modal"> fecha o modal
    document.querySelectorAll('dialog.modal').forEach(modal => {
        modal.addEventListener('click', evento => {
            if (evento.target === modal) modal.close();
        });
    });
});

// Utilitarios compartilhados pelas telas do PediuPatrao
const PP = {

    // Token CSRF publicado pelo layout. Vai no cabecalho de todo POST/PUT/DELETE feito via fetch.
    cabecalhoCsrf() {
        const token = document.querySelector('meta[name="csrf-token"]');
        const nome = document.querySelector('meta[name="csrf-header"]');
        return token && nome ? { [nome.content]: token.content } : {};
    },

    // Chama a API. Sucesso: devolve o JSON (ou {} se nao houver corpo). Erro: avisa e devolve null.
    async api(url, metodo = 'GET', corpo) {
        const opcoes = { method: metodo, headers: { Accept: 'application/json', ...PP.cabecalhoCsrf() } };
        if (corpo !== undefined) {
            opcoes.headers['Content-Type'] = 'application/json';
            opcoes.body = JSON.stringify(corpo);
        }
        const resposta = await fetch(url, opcoes);
        if (resposta.ok) {
            return resposta.status === 204 ? {} : resposta.json();
        }
        PP.avisar(await PP.lerProblema(resposta), 'danger');
        return null;
    },

    // O backend responde erros no formato ProblemDetail: { detail, codigo, erros? }
    async lerProblema(resposta) {
        try {
            const problema = await resposta.json();
            const erros = problema.erros ? ' (' + problema.erros.join('; ') + ')' : '';
            return (problema.detail || 'Erro ' + resposta.status) + erros;
        } catch (e) {
            console.warn('Resposta de erro sem JSON', e);
            return 'Erro ' + resposta.status;
        }
    },

    // Aviso flutuante no canto da tela (some sozinho). tipo: 'success' ou 'danger'
    avisar(mensagem, tipo = 'success') {
        const aviso = document.createElement('div');
        aviso.className = 'aviso ' + (tipo === 'danger' ? 'aviso-erro' : 'aviso-ok');
        aviso.setAttribute('role', tipo === 'danger' ? 'alert' : 'status');
        aviso.textContent = mensagem;
        document.getElementById('avisos').appendChild(aviso);
        setTimeout(() => aviso.remove(), 7000);
    },

    // Escapa texto antes de ir para innerHTML (protege contra XSS)
    texto(valor) {
        const trocas = { '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;' };
        return String(valor ?? '').replace(/[&<>"']/g, c => trocas[c]);
    },

    moeda(valor) {
        return Number(valor).toLocaleString('pt-BR', { style: 'currency', currency: 'BRL' });
    },

    // Icone do sprite /images/icones.svg, para HTML montado no JS
    icone(nome) {
        return `<svg class="ic" aria-hidden="true"><use href="/images/icones.svg#${nome}"></use></svg>`;
    }
};
