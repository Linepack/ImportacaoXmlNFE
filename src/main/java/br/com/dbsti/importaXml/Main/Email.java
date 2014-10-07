/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package br.com.dbsti.importaXml.Main;

import br.com.dbsti.importaXml.parse.Leitor;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
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

    private static String nomeDoArquivo;
    private static String nomeDoArquivoXml;
    private static String nomeDoArquivoPdf;
    private static String diretorio;

    public void execute(String hostEmail,
            String protocoloEmail,
            Integer porta,
            String usuario,
            String senha,
            String diretorioXml,
            String pastaBackupMensagens,
            String pastaErroMensagens) throws IOException {

        try {
            diretorio = diretorioXml;

            Properties props = new Properties();
            Session session = Session.getDefaultInstance(props, null);
            Store store = session.getStore(protocoloEmail);
            store.connect(hostEmail, porta, usuario, senha);
            Folder folder = store.getFolder("INBOX");
            folder.open(Folder.READ_WRITE);

            Folder folderBackup = store.getFolder(pastaBackupMensagens);
            folderBackup.open(Folder.READ_WRITE);

            Folder folderErro = store.getFolder(pastaErroMensagens);
            folderErro.open(Folder.READ_WRITE);

            for (Message message : folder.getMessages()) {

                nomeDoArquivoXml = null;
                nomeDoArquivoPdf = null;
                nomeDoArquivo = null;
                
                Log.gravaLog("Novo Email recebido. Remetente: " + message.getFrom()[0] + ", Assunto: " + message.getSubject());

                Part parteMensagem = message;
                Object content = parteMensagem.getContent();

                if (content instanceof Multipart) {
                    MimeMultipart mmp = (MimeMultipart) message.getContent();
                    for (int contador = 0; contador < mmp.getCount(); contador++) {
                        parteMensagem = ((Multipart) mmp).getBodyPart(contador);
                        String contentType = parteMensagem.getContentType();
                        downloadAnexo(contentType, mmp, contador);
                    }
                    if (nomeDoArquivoXml != null) {
                        Leitor.ler(diretorio + nomeDoArquivoXml, diretorio + nomeDoArquivoPdf);
                    }
                }

                if (Leitor.algoErrado) {
                    moveMensagem(message, folder, folderErro);
                } else {
                    moveMensagem(message, folder, folderBackup);
                }
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

    private static void downloadAnexo(String contentType, MimeMultipart multi, Integer indexMultPart) throws IOException, MessagingException {

        if (!contentType.startsWith("text/plain") & !contentType.startsWith("text/html")) {

            nomeDoArquivo = multi.getBodyPart(indexMultPart).getFileName();
            if (nomeDoArquivo != null) {
                Log.gravaLog("Anexo Encontrado: " + nomeDoArquivo);
            }

            byte[] buf = new byte[4096];
            if (nomeDoArquivo != null && nomeDoArquivo.toUpperCase().contains("PDF")) {
                InputStream is = multi.getBodyPart(indexMultPart).getInputStream();
                FileOutputStream fos = new FileOutputStream(diretorio + nomeDoArquivo);
                int bytesRead;
                while ((bytesRead = is.read(buf)) != -1) {
                    fos.write(buf, 0, bytesRead);
                }
                fos.close();
                nomeDoArquivoPdf = nomeDoArquivo;
                Log.gravaLog("Download do PDF da nota " + nomeDoArquivoPdf + " realizado com sucesso.");
            } else if (nomeDoArquivo != null && nomeDoArquivo.toUpperCase().contains("XML")) {
                InputStream is = multi.getBodyPart(indexMultPart).getInputStream();
                FileOutputStream fos = new FileOutputStream(diretorio + nomeDoArquivo);
                int bytesRead;
                while ((bytesRead = is.read(buf)) != -1) {
                    fos.write(buf, 0, bytesRead);
                }
                nomeDoArquivoXml = nomeDoArquivo;
                fos.close();
                Log.gravaLog("Download do XML da nota " + nomeDoArquivoXml + " realizado com sucesso.");
            }
        }
    }

    private static void moveMensagem(Message message, Folder folderOrigem, Folder folderDestino) throws IOException {
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
