/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package br.com.dbsti.importaXml.Main;

import br.com.dbsti.importaXml.parse.Leitor;
import java.io.BufferedReader;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Properties;
import javax.mail.Flags;
import javax.mail.Folder;
import javax.mail.FolderClosedException;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Multipart;
import javax.mail.Part;
import javax.mail.Session;
import javax.mail.Store;
import javax.mail.internet.MimeMultipart;

/**
 *
 * @author Franciscato
 */
public class Email {

    public void execute(String hostEmail,
            String protocoloEmail,
            Integer porta,
            String usuario,
            String senha,
            String diretorioXml,
            String pastaBackupMensagens) throws IOException {

        try {

            String nomeDoArquivo = null;
            String nomeDoArquivoXml = null;
            String nomeDoArquivoPdf = null;

            Properties props = new Properties();
            Session session = Session.getDefaultInstance(props, null);
            Store store = session.getStore(protocoloEmail);
            store.connect(hostEmail, porta, usuario, senha);
            Folder folder = store.getFolder("INBOX");
            folder.open(Folder.READ_WRITE);

            Folder folderBackup = store.getFolder(pastaBackupMensagens);
            folderBackup.open(Folder.READ_WRITE);

            for (Message message : folder.getMessages()) {

                Log.gravaLog("Novo Email recebido... ");

                Part parteMensagem = message;
                Object content = parteMensagem.getContent();

                if (content instanceof Multipart) {
                    MimeMultipart mmp = (MimeMultipart) message.getContent();
                    for (int contador = 0; contador < mmp.getCount(); contador++) {
                        parteMensagem = ((Multipart) mmp).getBodyPart(contador);
                        String contentType = parteMensagem.getContentType();
                        downloadAnexo(contentType,
                                parteMensagem,
                                diretorioXml,
                                content,
                                nomeDoArquivo,
                                nomeDoArquivoPdf,
                                nomeDoArquivoXml);
                    }
                }
                moveMensagemLida(message, folder, folderBackup);
            }

            folder.close(true);
            folderBackup.close(true);
            store.close();
        } catch (FolderClosedException f) {
            Log.gravaLog("ERRO FolderClosedException: " + f.getMessage());
        } catch (MessagingException m) {
            Log.gravaLog("ERRO MessagingException: " + m.getMessage());
        } catch (IOException i) {
            Log.gravaLog("ERRO IOException: " + i.getMessage());
        }
    }

    private static void downloadAnexo(String contentType,
            Part parteMensagem,
            String diretorioXml,
            Object content,
            String nomeDoArquivo,
            String nomeDoArquivoPdf,
            String nomeDoArquivoXml) throws IOException, MessagingException {

        if (!contentType.startsWith("text/plain") & !contentType.startsWith("text/html")) {
                    
            Log.gravaLog("Anexo Encontrado... ");
            byte[] buf = new byte[4096];

            String caminhoBase = diretorioXml;
            Multipart multi = (Multipart) content;

            for (int i = 0; i < multi.getCount(); i++) {
                nomeDoArquivo = multi.getBodyPart(i).getFileName();
                if (nomeDoArquivo != null && nomeDoArquivo.contains("pdf")) {
                    InputStream is = multi.getBodyPart(i).getInputStream();
                    FileOutputStream fos = new FileOutputStream(caminhoBase + nomeDoArquivo);
                    int bytesRead;
                    while ((bytesRead = is.read(buf)) != -1) {
                        fos.write(buf, 0, bytesRead);
                    }
                    fos.close();
                    nomeDoArquivoPdf = nomeDoArquivo;
                    Log.gravaLog("Download do PDF da nota " + nomeDoArquivoPdf + " realizado com sucesso.");
                } else if (nomeDoArquivo != null && nomeDoArquivo.contains("xml")) {
                    InputStream is = multi.getBodyPart(i).getInputStream();
                    FileOutputStream fos = new FileOutputStream(caminhoBase + nomeDoArquivo);
                    int bytesRead;
                    while ((bytesRead = is.read(buf)) != -1) {
                        fos.write(buf, 0, bytesRead);
                    }
                    nomeDoArquivoXml = nomeDoArquivo;
                    fos.close();
                    Log.gravaLog("Download do XML da nota " + nomeDoArquivoXml + " realizado com sucesso.");
                }
            }
            if (nomeDoArquivoXml != null) {
                Leitor.ler(caminhoBase + nomeDoArquivoXml, caminhoBase + nomeDoArquivoPdf);
            }
        }
    }

    private static void moveMensagemLida(Message message, Folder folderOrigem, Folder folderDestino) throws IOException {
        try {
            message.setFlag(Flags.Flag.SEEN, true);
            Message[] mensagemCopia = new Message[1];
            mensagemCopia[0] = message;
            folderOrigem.copyMessages(mensagemCopia, folderDestino);
            message.setFlag(Flags.Flag.DELETED, true);
        } catch (MessagingException ex) {
            Log.gravaLog(ex.getMessage());
        }
    }

}
