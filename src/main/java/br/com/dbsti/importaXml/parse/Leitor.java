package br.com.dbsti.importaXml.parse;

/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
import br.com.dbsti.importaXml.Main.Log;
import br.com.dbsti.importaXml.model.Destinatario;
import br.com.dbsti.importaXml.model.Emitente;
import br.com.dbsti.importaXml.model.EnderecoEmitente;
import br.com.dbsti.importaXml.model.EntityManagerDAO;
import br.com.dbsti.importaXml.model.Nota;
import br.com.dbsti.importaXml.model.Pagamento;
import br.com.dbsti.importaXml.model.Produto;
import br.com.dbsti.importaXml.model.Transportador;
import br.com.dbsti.importaXml.model.Tributo;
import br.inf.portalfiscal.nfe.TNFe.InfNFe.Cobr.Dup;
import br.inf.portalfiscal.nfe.TNFe.InfNFe.Dest;
import br.inf.portalfiscal.nfe.TNFe.InfNFe.Det;
import br.inf.portalfiscal.nfe.TNFe.InfNFe.Det.Imposto.COFINS;
import br.inf.portalfiscal.nfe.TNFe.InfNFe.Emit;
import br.inf.portalfiscal.nfe.TNFe.InfNFe.Ide;
import br.inf.portalfiscal.nfe.TNFe.InfNFe.Transp.Transporta;
import br.inf.portalfiscal.nfe.TNfeProc;
import br.inf.portalfiscal.nfe.TProtNFe.InfProt;
import java.io.File;
import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.persistence.EntityManager;
import javax.persistence.Query;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;

/**
 *
 * @author Flavia
 */
public class Leitor {

    private static EntityManager em;
    public static Boolean algoErrado = false;

    public static void ler(String pathArquivoXml, String pathArquivoPdf) throws IOException {

        try {
            Log.gravaLog("Realizando Parse do Xml... ");
            File file = new File(pathArquivoXml);

            JAXBContext contexto = JAXBContext.newInstance(TNfeProc.class);
            Unmarshaller u = contexto.createUnmarshaller();
            TNfeProc notaFiscal = (TNfeProc) u.unmarshal(file);

            em = EntityManagerDAO.getEntityManager();
            parse(notaFiscal, pathArquivoXml, pathArquivoPdf);
            em.close();

            if (!algoErrado) {
                Log.gravaLog("Parse realizado com sucesso! ");
            }

        } catch (IOException ex) {
            algoErrado = true;
            em.getTransaction().rollback();
            Logger.getLogger(Leitor.class.getName()).log(Level.SEVERE, null, ex);
        } catch (JAXBException ex) {
            algoErrado = true;
            em.getTransaction().rollback();
            Logger.getLogger(Leitor.class.getName()).log(Level.SEVERE, null, ex);
            Log.gravaLog(ex.getMessage());
        }

    }

    private static void parse(TNfeProc nfe, String pathXml, String pathPdf) throws IOException {

        Nota nfeMestre = new Nota();
        Query query = em.createQuery("select n from Nota n where n.chaveAcesso = '" + nfe.getProtNFe().getInfProt().getChNFe() + "'");

        for (Object object : query.getResultList()) {
            nfeMestre = (Nota) object;
        }

        if (nfeMestre.getChaveAcesso() != null) {            
            Log.gravaLog("Nota Fiscal já existente, verifique!");
        } else {

            try {
                InfProt inf = nfe.getProtNFe().getInfProt();
                nfeMestre.setChaveAcesso(inf.getChNFe());
                SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
                Date dataRecebimento = new Date(dateFormat.parse(inf.getDhRecbto()).getTime());
                nfeMestre.setDataRecebimento(dataRecebimento);
                nfeMestre.setMensagem(inf.getXMotivo());
                nfeMestre.setNumeroProtocolo(inf.getNProt());
                nfeMestre.setSituacao(inf.getCStat());

                Ide ide = nfe.getNFe().getInfNFe().getIde();
                nfeMestre.setCodigoAmbiente(Integer.parseInt(ide.getTpAmb()));
                nfeMestre.setModelo(ide.getMod());
                nfeMestre.setNumeroNota(Integer.parseInt(ide.getNNF()));
                nfeMestre.setSerie(ide.getSerie());

                SimpleDateFormat dateFormatSomeDay = new SimpleDateFormat("yyyy-MM-dd");

                if (ide.getDhEmi() != null) {
                    Date dataEmissao = new Date(dateFormatSomeDay.parse(ide.getDhEmi()).getTime());
                    nfeMestre.setDataHoraEmissao(dataEmissao);
                }

                if (ide.getDhSaiEnt() != null) {
                    Date dataSaida = new Date(dateFormatSomeDay.parse(ide.getDhSaiEnt()).getTime());
                    nfeMestre.setDataHoraSaida(dataSaida);
                }

                nfeMestre.setCamihhoXml(pathXml);
                nfeMestre.setCaminhoPdf(pathPdf);

                Emitente emitente = parseEmitente(nfe.getNFe().getInfNFe().getEmit());
                nfeMestre.setNfeEmitente(emitente);

                Destinatario destinatario = parseDestinatario(nfe.getNFe().getInfNFe().getDest());
                nfeMestre.setDestinatario(destinatario);

                if (nfe.getNFe().getInfNFe().getTransp().getTransporta() != null) {
                    Transportador transportador = parseTransportador(nfe.getNFe().getInfNFe().getTransp().getTransporta());
                    nfeMestre.setTransportador(transportador);
                }

                em.persist(nfeMestre);

                if (nfe.getNFe().getInfNFe().getCobr() != null) {
                    parsePagamento(nfe.getNFe().getInfNFe().getCobr().getDup(), nfeMestre);
                }

                parseProduto(nfe.getNFe().getInfNFe().getDet(), nfeMestre);

                if (!algoErrado) {
                    em.getTransaction().commit();
                }

            } catch (ParseException ex) {
                algoErrado = true;
                em.getTransaction().rollback();
                Logger.getLogger(Leitor.class.getName()).log(Level.SEVERE, null, ex);
            }
        }

    }

    private static Emitente parseEmitente(Emit emit) {

        Emitente nfeEmitente = new Emitente();

        Query query = em.createQuery("select e "
                + "from Emitente e "
                + "where e.cnpj = '" + emit.getCNPJ() + "'");

        for (Object object : query.getResultList()) {
            nfeEmitente = (Emitente) object;
        }

        if (nfeEmitente.getId() == null) {

            if (emit.getCNAE() != null) {
                nfeEmitente.setCnae(emit.getCNAE());
            }
            if (emit.getCNPJ() != null) {
                nfeEmitente.setCnpj(emit.getCNPJ());
            }
            if (emit.getCPF() != null) {
                nfeEmitente.setCpf(emit.getCPF());
            }
            if (emit.getCRT() != null) {
                nfeEmitente.setCrt(emit.getCRT());
            }
            if (emit.getIE() != null) {
                nfeEmitente.setIe(emit.getIE());
            }
            nfeEmitente.setNomeFantasia(emit.getXFant());
            nfeEmitente.setRazaoSocial(emit.getXNome());

            EnderecoEmitente enderecoEmitente = new EnderecoEmitente();
            enderecoEmitente.setBairro(emit.getEnderEmit().getXBairro());
            enderecoEmitente.setCep(Integer.parseInt(emit.getEnderEmit().getCEP()));
            enderecoEmitente.setCodIbgeMunicipio(Integer.parseInt(emit.getEnderEmit().getCMun()));
            enderecoEmitente.setCodIbgePais(Integer.parseInt(emit.getEnderEmit().getCPais()));
            enderecoEmitente.setComplemento(emit.getEnderEmit().getXCpl());
            enderecoEmitente.setLogradouro(emit.getEnderEmit().getXLgr());
            enderecoEmitente.setNomeMunicipio(emit.getEnderEmit().getXMun());
            enderecoEmitente.setNomePais(emit.getEnderEmit().getXPais());
            enderecoEmitente.setNumero(emit.getEnderEmit().getNro());
            enderecoEmitente.setSiglaEstado(emit.getEnderEmit().getUF().value());
            enderecoEmitente.setTelefone(emit.getEnderEmit().getFone());

            nfeEmitente.setEnderecoEmitente(enderecoEmitente);

            em.persist(enderecoEmitente);
            em.persist(nfeEmitente);
        }

        return nfeEmitente;
    }

    private static Destinatario parseDestinatario(Dest dest) {
        Destinatario destinatario = new Destinatario();

        Query query = em.createQuery("select d from Destinatario d where d.cnpj = '" + dest.getCNPJ() + "'");

        for (Object object : query.getResultList()) {
            destinatario = (Destinatario) object;
        }

        if (destinatario.getCnpj() == null) {
            destinatario.setCnpj(dest.getCNPJ());
            em.persist(destinatario);
        }

        return destinatario;
    }

    private static Transportador parseTransportador(Transporta transporta) {

        Transportador transportador = new Transportador();

        Query query = em.createQuery("select t from Transportador t where t.cnpj = '" + transporta.getCNPJ() + "'");

        for (Object object : query.getResultList()) {
            transportador = (Transportador) object;
        }

        if (transportador.getCnpj() == null) {
            transportador.setCnpj(transporta.getCNPJ());
            transportador.setEndereco(transporta.getXEnder());
            transportador.setIe(transporta.getIE());
            transportador.setNomeMunicipio(transporta.getXMun());
            transportador.setRazaoSocial(transporta.getXNome());
            transportador.setSiglaEstado(transporta.getUF().value());

            em.persist(transportador);
        }

        return transportador;
    }

    private static void parsePagamento(List<Dup> duplicatas, Nota nota) throws ParseException {

        for (Dup duplicata : duplicatas) {
            Pagamento pagamento = new Pagamento();
            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
            Date dataVencimento = new Date(dateFormat.parse(duplicata.getDVenc()).getTime());
            pagamento.setDataPagamento(dataVencimento);
            pagamento.setNumeroPagamento(duplicata.getNDup());
            pagamento.setValorPagamento(Double.parseDouble(duplicata.getVDup()));
            pagamento.setNota(nota);

            em.persist(pagamento);
        }
    }

    private static void parseProduto(List<Det> detalhes, Nota nota) throws IOException {

        for (Det detalhe : detalhes) {

            Produto produto = new Produto();

            produto.setCfop(Integer.parseInt(detalhe.getProd().getCFOP()));
            produto.setCodigo(detalhe.getProd().getCProd());
            produto.setCodigoBarras(detalhe.getProd().getCEAN());
            produto.setDescricao(detalhe.getProd().getXProd());
            produto.setDescricaoItemPedido(detalhe.getProd().getNItemPed());
            produto.setDescricaoPedido(detalhe.getProd().getXPed());
            produto.setNcm(Integer.parseInt(detalhe.getProd().getNCM()));
            produto.setQuantidade(Double.parseDouble(detalhe.getProd().getQCom()));
            produto.setUnidadeMedida(detalhe.getProd().getUCom());
            produto.setSequenciaItemNota(Integer.parseInt(detalhe.getNItem()));

            if (detalhe.getProd().getVDesc() != null) {
                produto.setValorDesconto(Double.parseDouble(detalhe.getProd().getVDesc()));
            }
            if (detalhe.getProd().getVFrete() != null) {
                produto.setValorFrete(Double.parseDouble(detalhe.getProd().getVFrete()));
            }
            if (detalhe.getProd().getVOutro() != null) {
                produto.setValorOutros(Double.parseDouble(detalhe.getProd().getVOutro()));
            }

            produto.setValorUnitario(Double.parseDouble(detalhe.getProd().getVUnCom()));
            produto.setValorTotal(Double.parseDouble(detalhe.getProd().getVProd()));
            produto.setNota(nota);

            em.persist(produto);

            parseTributoCofins(detalhe.getImposto().getContent(), produto);

        }

    }

    private static void parseTributoCofins(List<JAXBElement<?>> jaxbImpostos, Produto produto) throws IOException {
        
        COFINS cofins;
        for (Object object : jaxbImpostos) {
            try {
                File file = new File("imposto.xml");
                JAXBContext contexto = JAXBContext.newInstance(COFINS.class);
                Marshaller m = contexto.createMarshaller();
                m.marshal(object, file);

                Unmarshaller u = contexto.createUnmarshaller();
                cofins = (COFINS) u.unmarshal(file);

                Tributo tributoCofins = new Tributo();
                tributoCofins.setAliquota(Double.parseDouble(cofins.getCOFINSAliq().getPCOFINS()));
                tributoCofins.setBaseCalculo(Double.parseDouble(cofins.getCOFINSAliq().getVBC()));
                tributoCofins.setCst(cofins.getCOFINSAliq().getCST());
                tributoCofins.setNome("COFINS");
                tributoCofins.setProduto(produto);
                tributoCofins.setValor(Double.parseDouble(cofins.getCOFINSAliq().getVCOFINS()));
                em.persist(tributoCofins);

            } catch (JAXBException ex) {

            } catch (Exception ex) {
                algoErrado = true;
                em.getTransaction().rollback();
                Logger.getLogger(Leitor.class.getName()).log(Level.SEVERE, null, ex);
                Log.gravaLog(ex.getMessage());
            }

        }

    }

}
