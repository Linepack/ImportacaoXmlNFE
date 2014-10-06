/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package br.com.dbsti.importaXml.Main;

import br.com.dbsti.importaXml.model.Configuracoes;
import br.com.dbsti.importaXml.model.EntityManagerDAO;
import java.io.IOException;
import java.util.Timer;
import java.util.TimerTask;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.persistence.EntityManager;
import javax.persistence.Query;

/**
 *
 * @author Franciscato
 */
public class Tarefa {

    private static Configuracoes config;
    public static String PATH_LOG;

    public static void main(String[] args) {
        try {
            buscaConfiguracoes();
            if (config.getHostEmail() == null) {
                Log.gravaLog("Você precisa configurar seu BANCO DE DADOS, verifique a tabela CONFIGURACOES.");
            } else {
                certificaConexao();
                PATH_LOG = config.getDiretorioProjeto();
                executaTimer();
            }
        } catch (Exception ex) {
            try {
                Log.gravaLog(ex.getMessage());
            } catch (IOException ex1) {
                Logger.getLogger(Tarefa.class.getName()).log(Level.SEVERE, null, ex1);
            }
        }
    }

    private static void buscaConfiguracoes() {
        config = new Configuracoes();
        EntityManager em = EntityManagerDAO.getEntityManager();
        Query query = em.createQuery("select c from Configuracoes c");

        for (Object c : query.getResultList()) {
            config = (Configuracoes) c;
        }
    }

    private static void certificaConexao() throws Exception {
        System.clearProperty("javax.net.ssl.trustStoreType");
        System.clearProperty("javax.net.ssl.trustStore");
        System.clearProperty("javax.net.ssl.trustStorePassword");

        String[] hostEmail = new String[1];
        hostEmail[0] = config.getHostCertificado();
        InstallCert.instalaCertificado(hostEmail);

        System.setProperty("javax.net.ssl.trustStoreType", "JKS");
        System.setProperty("javax.net.ssl.trustStore", config.getDiretorioProjeto() + "jssecacerts");
        System.setProperty("javax.net.ssl.trustStorePassword", "changeit");
    }

    private static void executaTimer() throws IOException {
        Log.gravaLog("Verificando Email's... ");
        Timer timer = null;
        if (timer == null) {
            timer = new Timer();
            TimerTask tarefa;

            tarefa = new TimerTask() {

                @Override
                public void run() {

                    try {
                        Email email = new Email();
                        email.execute(config.getHostEmail(),
                                config.getProtocoloLeitura(),
                                config.getPortaProtocolo(),
                                config.getUsuario(),
                                config.getSenha(),
                                config.getDiretorioXml(),
                                config.getPastaBackupMensagens());
                    } catch (IOException ex) {
                        Logger.getLogger(Tarefa.class.getName()).log(Level.SEVERE, null, ex);
                    }
                }
            };
            timer.scheduleAtFixedRate(tarefa, config.getSegundosIntervaloLeitura(), config.getSegundosIntervaloLeitura());
        }
    }

}
