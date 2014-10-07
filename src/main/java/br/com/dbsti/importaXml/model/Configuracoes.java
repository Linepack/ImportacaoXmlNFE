/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package br.com.dbsti.importaXml.model;

import java.io.Serializable;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;

/**
 *
 * @author Franciscato
 */
@Entity
public class Configuracoes implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

    @Column
    private String hostCertificado;

    @Column
    private String hostEmail;

    @Column
    private String diretorioProjeto;

    @Column
    private String protocoloLeitura;

    @Column
    private Integer segundosIntervaloLeitura;

    @Column
    private String usuario;

    @Column
    private String senha;

    @Column
    private String diretorioXml;
    
    private String pastaBackupMensagens;
    
    private Integer portaProtocolo;
    
    private String pastaErroMensagens;

    public String getPastaErroMensagens() {
        return pastaErroMensagens;
    }

    public void setPastaErroMensagens(String pastaErroMensagens) {
        this.pastaErroMensagens = pastaErroMensagens;
    }
    
    

    public String getPastaBackupMensagens() {
        return pastaBackupMensagens;
    }

    public void setPastaBackupMensagens(String pastaBackupMensagens) {
        this.pastaBackupMensagens = pastaBackupMensagens;
    }

    public Integer getPortaProtocolo() {
        return portaProtocolo;
    }

    public void setPortaProtocolo(Integer portaProtocolo) {
        this.portaProtocolo = portaProtocolo;
    }
    
    
    

    public String getHostCertificado() {
        return hostCertificado;
    }

    public void setHostCertificado(String hostCertificado) {
        this.hostCertificado = hostCertificado;
    }

    public String getHostEmail() {
        return hostEmail;
    }

    public void setHostEmail(String hostEmail) {
        this.hostEmail = hostEmail;
    }

    public String getProtocoloLeitura() {
        return protocoloLeitura;
    }

    public void setProtocoloLeitura(String protocoloLeitura) {
        this.protocoloLeitura = protocoloLeitura;
    }

    public String getUsuario() {
        return usuario;
    }

    public void setUsuario(String usuario) {
        this.usuario = usuario;
    }

    public String getSenha() {
        return senha;
    }

    public void setSenha(String senha) {
        this.senha = senha;
    }

    public String getDiretorioXml() {
        return diretorioXml;
    }

    public void setDiretorioXml(String diretorioXml) {
        this.diretorioXml = diretorioXml;
    }

    public Integer getSegundosIntervaloLeitura() {
        return segundosIntervaloLeitura;
    }

    public void setSegundosIntervaloLeitura(Integer segundosIntervaloLeitura) {
        this.segundosIntervaloLeitura = segundosIntervaloLeitura;
    }

    public String getDiretorioProjeto() {
        return diretorioProjeto;
    }

    public void setDiretorioProjeto(String diretorioProjeto) {
        this.diretorioProjeto = diretorioProjeto;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    @Override
    public int hashCode() {
        int hash = 0;
        hash += (id != null ? id.hashCode() : 0);
        return hash;
    }

    @Override
    public boolean equals(Object object) {
        // TODO: Warning - this method won't work in the case the id fields are not set
        if (!(object instanceof Configuracoes)) {
            return false;
        }
        Configuracoes other = (Configuracoes) object;
        if ((this.id == null && other.id != null) || (this.id != null && !this.id.equals(other.id))) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "br.com.dbsti.importaXml.model.Configuracoes[ id=" + id + " ]";
    }

}
