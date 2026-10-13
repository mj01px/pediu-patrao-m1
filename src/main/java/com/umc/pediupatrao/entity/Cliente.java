package com.umc.pediupatrao.entity;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "clientes")
public class Cliente {

    @Id
    private String id;

    @NotBlank(message = "nome é obrigatório")
    @Size(max = 120)
    private String nome;

    @NotBlank(message = "telefone é obrigatório")
    @Size(max = 20)
    private String telefone;

    // Endereço completo
    @Size(max = 9)
    private String cep;
    @Size(max = 150)
    private String logradouro;
    @Size(max = 10)
    private String numero;
    @Size(max = 80)
    private String complemento;
    @Size(max = 80)
    private String bairro;
    @Size(max = 80)
    private String cidade;
    @Size(max = 2)
    private String estado;

    // Mantido para compatibilidade com dados antigos
    private String endereco;

    /** Copia os dados cadastrais de outro cliente (nunca o id). */
    public void atualizarCom(Cliente dados) {
        this.nome = dados.nome;
        this.telefone = dados.telefone;
        this.cep = dados.cep;
        this.logradouro = dados.logradouro;
        this.numero = dados.numero;
        this.complemento = dados.complemento;
        this.bairro = dados.bairro;
        this.cidade = dados.cidade;
        this.estado = dados.estado;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getNome() { return nome; }
    public void setNome(String nome) { this.nome = nome; }

    public String getTelefone() { return telefone; }
    public void setTelefone(String telefone) { this.telefone = telefone; }

    public String getCep() { return cep; }
    public void setCep(String cep) { this.cep = cep; }

    public String getLogradouro() { return logradouro; }
    public void setLogradouro(String logradouro) { this.logradouro = logradouro; }

    public String getNumero() { return numero; }
    public void setNumero(String numero) { this.numero = numero; }

    public String getComplemento() { return complemento; }
    public void setComplemento(String complemento) { this.complemento = complemento; }

    public String getBairro() { return bairro; }
    public void setBairro(String bairro) { this.bairro = bairro; }

    public String getCidade() { return cidade; }
    public void setCidade(String cidade) { this.cidade = cidade; }

    public String getEstado() { return estado; }
    public void setEstado(String estado) { this.estado = estado; }

    public String getEndereco() { return endereco; }
    public void setEndereco(String endereco) { this.endereco = endereco; }
}
