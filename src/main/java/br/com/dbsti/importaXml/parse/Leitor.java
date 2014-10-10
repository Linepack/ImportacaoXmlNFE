package br.com.dbsti.importaXml.parse;

/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
import br.com.dbsti.importaXml.Main.Log;
import br.com.dbsti.importaXml.Main.Tarefa;
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
import br.inf.portalfiscal.nfe.TNFe.InfNFe.Det.Imposto.PIS;
import br.inf.portalfiscal.nfe.TIpi;
import br.inf.portalfiscal.nfe.TNFe.InfNFe.Det.Imposto.COFINSST;
import br.inf.portalfiscal.nfe.TNFe.InfNFe.Det.Imposto.ICMS;
import br.inf.portalfiscal.nfe.TNFe.InfNFe.Det.Imposto.II;
import br.inf.portalfiscal.nfe.TNFe.InfNFe.Det.Imposto.ISSQN;
import br.inf.portalfiscal.nfe.TNFe.InfNFe.Det.Imposto.PISST;
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
                if (pathPdf != null) {
                    nfeMestre.setCaminhoPdf(pathPdf);
                }

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
            if (emit.getEnderEmit().getCEP() != null) {
                enderecoEmitente.setCep(Integer.parseInt(emit.getEnderEmit().getCEP()));
            }
            if (emit.getEnderEmit().getCMun() != null) {
                enderecoEmitente.setCodIbgeMunicipio(Integer.parseInt(emit.getEnderEmit().getCMun()));
            }
            if (emit.getEnderEmit().getCPais() != null) {
                enderecoEmitente.setCodIbgePais(Integer.parseInt(emit.getEnderEmit().getCPais()));
            }
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
            parseTributoCofinsST(detalhe.getImposto().getContent(), produto);
            parseTributoPis(detalhe.getImposto().getContent(), produto);
            parseTributoPisST(detalhe.getImposto().getContent(), produto);
            parseTributoIpi(detalhe.getImposto().getContent(), produto);
            parseTributoISSQN(detalhe.getImposto().getContent(), produto);
            parseTributoII(detalhe.getImposto().getContent(), produto);
            parseTributoIcms(detalhe.getImposto().getContent(), produto);
        }

    }

    private static void parseTributoCofins(List<JAXBElement<?>> jaxbImpostos, Produto produto) throws IOException {

        COFINS cofins;
        for (Object object : jaxbImpostos) {
            try {
                File file = new File(Tarefa.PATH_LOG + "cofins.xml");
                JAXBContext contexto = JAXBContext.newInstance(COFINS.class);
                Marshaller m = contexto.createMarshaller();
                m.marshal(object, file);

                Unmarshaller u = contexto.createUnmarshaller();
                cofins = (COFINS) u.unmarshal(file);

                Tributo tributoCofins = new Tributo();

                if (cofins.getCOFINSAliq() != null) {
                    tributoCofins.setAliquota(Double.parseDouble(cofins.getCOFINSAliq().getPCOFINS()));
                    tributoCofins.setBaseCalculo(Double.parseDouble(cofins.getCOFINSAliq().getVBC()));
                    tributoCofins.setCst(cofins.getCOFINSAliq().getCST());
                    tributoCofins.setNome("COFINS");
                    tributoCofins.setProduto(produto);
                    tributoCofins.setValor(Double.parseDouble(cofins.getCOFINSAliq().getVCOFINS()));
                    em.persist(tributoCofins);
                }

            } catch (JAXBException ex) {

            } catch (Exception ex) {
                algoErrado = true;
                em.getTransaction().rollback();
                Logger.getLogger(Leitor.class.getName()).log(Level.SEVERE, null, ex);
                Log.gravaLog(ex.getMessage());
            }

        }

    }

    private static void parseTributoCofinsST(List<JAXBElement<?>> jaxbImpostos, Produto produto) throws IOException {

        COFINSST cofins;
        for (Object object : jaxbImpostos) {
            try {
                File file = new File(Tarefa.PATH_LOG + "cofinsst.xml");
                JAXBContext contexto = JAXBContext.newInstance(COFINSST.class);
                Marshaller m = contexto.createMarshaller();
                m.marshal(object, file);

                Unmarshaller u = contexto.createUnmarshaller();
                cofins = (COFINSST) u.unmarshal(file);

                Tributo tributoCofins = new Tributo();

                if (cofins != null) {
                    tributoCofins.setAliquota(Double.parseDouble(cofins.getPCOFINS()));
                    tributoCofins.setBaseCalculo(Double.parseDouble(cofins.getVBC()));
                    tributoCofins.setNome("COFINSST");
                    tributoCofins.setProduto(produto);
                    tributoCofins.setValor(Double.parseDouble(cofins.getVCOFINS()));
                    em.persist(tributoCofins);
                }

            } catch (JAXBException ex) {

            } catch (Exception ex) {
                algoErrado = true;
                em.getTransaction().rollback();
                Logger.getLogger(Leitor.class.getName()).log(Level.SEVERE, null, ex);
                Log.gravaLog(ex.getMessage());
            }

        }

    }

    private static void parseTributoPis(List<JAXBElement<?>> jaxbImpostos, Produto produto) throws IOException {

        PIS pis;
        for (Object object : jaxbImpostos) {
            try {
                File file = new File(Tarefa.PATH_LOG + "pis.xml");
                JAXBContext contexto = JAXBContext.newInstance(PIS.class);
                Marshaller m = contexto.createMarshaller();
                m.marshal(object, file);

                Unmarshaller u = contexto.createUnmarshaller();
                pis = (PIS) u.unmarshal(file);

                Tributo tributoPis = new Tributo();

                if (pis.getPISAliq() != null) {
                    tributoPis.setAliquota(Double.parseDouble(pis.getPISAliq().getPPIS()));
                    tributoPis.setBaseCalculo(Double.parseDouble(pis.getPISAliq().getVBC()));
                    tributoPis.setCst(pis.getPISAliq().getCST());
                    tributoPis.setNome("PIS");
                    tributoPis.setProduto(produto);
                    tributoPis.setValor(Double.parseDouble(pis.getPISAliq().getVPIS()));
                    em.persist(tributoPis);
                }

            } catch (JAXBException ex) {
            } catch (Exception ex) {
                algoErrado = true;
                em.getTransaction().rollback();
                Logger.getLogger(Leitor.class.getName()).log(Level.SEVERE, null, ex);
                Log.gravaLog(ex.getMessage());
            }

        }

    }

    private static void parseTributoPisST(List<JAXBElement<?>> jaxbImpostos, Produto produto) throws IOException {

        PISST pis;
        for (Object object : jaxbImpostos) {
            try {
                File file = new File(Tarefa.PATH_LOG + "pisst.xml");
                JAXBContext contexto = JAXBContext.newInstance(PISST.class);
                Marshaller m = contexto.createMarshaller();
                m.marshal(object, file);

                Unmarshaller u = contexto.createUnmarshaller();
                pis = (PISST) u.unmarshal(file);

                Tributo tributoPis = new Tributo();

                if (pis != null) {
                    tributoPis.setAliquota(Double.parseDouble(pis.getPPIS()));
                    tributoPis.setBaseCalculo(Double.parseDouble(pis.getVBC()));
                    tributoPis.setNome("PISST");
                    tributoPis.setProduto(produto);
                    tributoPis.setValor(Double.parseDouble(pis.getVPIS()));
                    em.persist(tributoPis);
                }

            } catch (JAXBException ex) {
            } catch (Exception ex) {
                algoErrado = true;
                em.getTransaction().rollback();
                Logger.getLogger(Leitor.class.getName()).log(Level.SEVERE, null, ex);
                Log.gravaLog(ex.getMessage());
            }

        }

    }

    private static void parseTributoIpi(List<JAXBElement<?>> jaxbImpostos, Produto produto) throws IOException {

        TIpi ipi;
        for (Object object : jaxbImpostos) {
            try {
                File file = new File(Tarefa.PATH_LOG + "ipi.xml");
                JAXBContext contexto = JAXBContext.newInstance(TIpi.class);
                Marshaller m = contexto.createMarshaller();
                m.marshal(object, file);

                Unmarshaller u = contexto.createUnmarshaller();
                ipi = (TIpi) u.unmarshal(file);

                Tributo tributoIpi = new Tributo();

                if (ipi.getIPITrib() != null) {
                    tributoIpi.setAliquota(Double.parseDouble(ipi.getIPITrib().getPIPI()));
                    tributoIpi.setBaseCalculo(Double.parseDouble(ipi.getIPITrib().getVBC()));
                    tributoIpi.setValor(Double.parseDouble(ipi.getIPITrib().getVIPI()));
                    tributoIpi.setCst(ipi.getIPITrib().getCST());
                    tributoIpi.setNome("IPI");
                    tributoIpi.setProduto(produto);
                    em.persist(tributoIpi);
                }

            } catch (JAXBException ex) {
            } catch (Exception ex) {
                algoErrado = true;
                em.getTransaction().rollback();
                Logger.getLogger(Leitor.class.getName()).log(Level.SEVERE, null, ex);
                Log.gravaLog(ex.getMessage());
            }

        }

    }

    private static void parseTributoISSQN(List<JAXBElement<?>> jaxbImpostos, Produto produto) throws IOException {

        ISSQN issqn;
        for (Object object : jaxbImpostos) {
            try {
                File file = new File(Tarefa.PATH_LOG + "issqn.xml");
                JAXBContext contexto = JAXBContext.newInstance(ISSQN.class);
                Marshaller m = contexto.createMarshaller();
                m.marshal(object, file);

                Unmarshaller u = contexto.createUnmarshaller();
                issqn = (ISSQN) u.unmarshal(file);

                Tributo tributoIssQn = new Tributo();

                if (issqn != null) {
                    tributoIssQn.setAliquota(Double.parseDouble(issqn.getVAliq()));
                    tributoIssQn.setBaseCalculo(Double.parseDouble(issqn.getVBC()));
                    tributoIssQn.setValor(Double.parseDouble(issqn.getVISSQN()));
                    tributoIssQn.setNome("ISSQN");
                    tributoIssQn.setProduto(produto);
                    em.persist(tributoIssQn);
                }

            } catch (JAXBException ex) {
            } catch (Exception ex) {
                algoErrado = true;
                em.getTransaction().rollback();
                Logger.getLogger(Leitor.class.getName()).log(Level.SEVERE, null, ex);
                Log.gravaLog(ex.getMessage());
            }

        }

    }

    private static void parseTributoII(List<JAXBElement<?>> jaxbImpostos, Produto produto) throws IOException {

        II ii;
        for (Object object : jaxbImpostos) {
            try {
                File file = new File(Tarefa.PATH_LOG + "ii.xml");
                JAXBContext contexto = JAXBContext.newInstance(II.class);
                Marshaller m = contexto.createMarshaller();
                m.marshal(object, file);

                Unmarshaller u = contexto.createUnmarshaller();
                ii = (II) u.unmarshal(file);

                Tributo tributoii = new Tributo();

                if (ii != null) {
                    tributoii.setBaseCalculo(Double.parseDouble(ii.getVBC()));
                    tributoii.setValor(Double.parseDouble(ii.getVII()));
                    tributoii.setNome("II");
                    tributoii.setProduto(produto);
                    em.persist(tributoii);
                }

            } catch (JAXBException ex) {
            } catch (Exception ex) {
                algoErrado = true;
                em.getTransaction().rollback();
                Logger.getLogger(Leitor.class.getName()).log(Level.SEVERE, null, ex);
                Log.gravaLog(ex.getMessage());
            }

        }

    }

    private static void parseTributoIcms(List<JAXBElement<?>> jaxbImpostos, Produto produto) throws IOException {

        ICMS icms;
        Boolean temIcms = false;
        for (Object object : jaxbImpostos) {
            try {
                File file = new File(Tarefa.PATH_LOG + "icms.xml");
                JAXBContext contexto = JAXBContext.newInstance(ICMS.class);
                Marshaller m = contexto.createMarshaller();
                m.marshal(object, file);

                Unmarshaller u = contexto.createUnmarshaller();
                icms = (ICMS) u.unmarshal(file);

                Tributo tributoIcms = new Tributo();

                if (icms.getICMS00() != null) {
                    if (icms.getICMS00().getPICMS() != null) {
                        tributoIcms.setAliquota(Double.parseDouble(icms.getICMS00().getPICMS()));
                    }
                    if (icms.getICMS00().getVBC() != null) {
                        tributoIcms.setBaseCalculo(Double.parseDouble(icms.getICMS00().getVBC()));
                    }
                    if (icms.getICMS00().getVICMS() != null) {
                        tributoIcms.setValor(Double.parseDouble(icms.getICMS00().getVICMS()));
                    }
                    tributoIcms.setCst(icms.getICMS00().getCST());
                    tributoIcms.setNome("ICMS");
                    tributoIcms.setProduto(produto);
                    temIcms = true;
                } else if (icms.getICMS10() != null) {
                    if (icms.getICMS10().getPICMS() != null) {
                        tributoIcms.setAliquota(Double.parseDouble(icms.getICMS10().getPICMS()));
                    }
                    if (icms.getICMS10().getVBC() != null) {
                        tributoIcms.setBaseCalculo(Double.parseDouble(icms.getICMS10().getVBC()));
                    }
                    if (icms.getICMS10().getVICMS() != null) {
                        tributoIcms.setValor(Double.parseDouble(icms.getICMS10().getVICMS()));
                    }
                    tributoIcms.setCst(icms.getICMS10().getCST());
                    tributoIcms.setNome("ICMS");
                    tributoIcms.setProduto(produto);
                    temIcms = true;
                } else if (icms.getICMS20() != null) {
                    if (icms.getICMS20().getPICMS() != null) {
                        tributoIcms.setAliquota(Double.parseDouble(icms.getICMS20().getPICMS()));
                    }
                    if (icms.getICMS20().getVBC() != null) {
                        tributoIcms.setBaseCalculo(Double.parseDouble(icms.getICMS20().getVBC()));
                    }
                    if (icms.getICMS20().getVICMS() != null) {
                        tributoIcms.setValor(Double.parseDouble(icms.getICMS20().getVICMS()));
                    }
                    tributoIcms.setCst(icms.getICMS20().getCST());
                    tributoIcms.setNome("ICMS");
                    tributoIcms.setProduto(produto);
                    temIcms = true;
                } else if (icms.getICMS30() != null) {
                    if (icms.getICMS30().getPICMSST() != null) {
                        tributoIcms.setAliquota(Double.parseDouble(icms.getICMS30().getPICMSST()));
                    }
                    if (icms.getICMS30().getVBCST() != null) {
                        tributoIcms.setBaseCalculo(Double.parseDouble(icms.getICMS30().getVBCST()));
                    }
                    if (icms.getICMS30().getVICMSST() != null) {
                        tributoIcms.setValor(Double.parseDouble(icms.getICMS30().getVICMSST()));
                    }
                    tributoIcms.setCst(icms.getICMS30().getCST());
                    tributoIcms.setNome("ICMS");
                    tributoIcms.setProduto(produto);
                    temIcms = true;
                } else if (icms.getICMS51() != null) {
                    if (icms.getICMS51().getPICMS() != null) {
                        tributoIcms.setAliquota(Double.parseDouble(icms.getICMS51().getPICMS()));
                    }
                    if (icms.getICMS51().getVBC() != null) {
                        tributoIcms.setBaseCalculo(Double.parseDouble(icms.getICMS51().getVBC()));
                    }
                    if (icms.getICMS51().getVICMS() != null) {
                        tributoIcms.setValor(Double.parseDouble(icms.getICMS51().getVICMS()));
                    }
                    tributoIcms.setCst(icms.getICMS51().getCST());
                    tributoIcms.setNome("ICMS");
                    tributoIcms.setProduto(produto);
                    temIcms = true;
                } else if (icms.getICMS70() != null) {
                    if (icms.getICMS70().getPICMS() != null) {
                        tributoIcms.setAliquota(Double.parseDouble(icms.getICMS70().getPICMS()));
                    }
                    if (icms.getICMS70().getVBC() != null) {
                        tributoIcms.setBaseCalculo(Double.parseDouble(icms.getICMS70().getVBC()));
                    }
                    if (icms.getICMS70().getVICMS() != null) {
                        tributoIcms.setValor(Double.parseDouble(icms.getICMS70().getVICMS()));
                    }
                    tributoIcms.setCst(icms.getICMS70().getCST());
                    tributoIcms.setNome("ICMS");
                    tributoIcms.setProduto(produto);
                    temIcms = true;
                } else if (icms.getICMS90() != null) {
                    if (icms.getICMS90().getPICMS() != null) {
                        tributoIcms.setAliquota(Double.parseDouble(icms.getICMS90().getPICMS()));
                    }
                    if (icms.getICMS90().getVBC() != null) {
                        tributoIcms.setBaseCalculo(Double.parseDouble(icms.getICMS90().getVBC()));
                    }
                    if (icms.getICMS90().getVICMS() != null) {
                        tributoIcms.setValor(Double.parseDouble(icms.getICMS90().getVICMS()));
                    }
                    tributoIcms.setCst(icms.getICMS90().getCST());
                    tributoIcms.setNome("ICMS");
                    tributoIcms.setProduto(produto);
                    temIcms = true;
                } else if (icms.getICMSPart() != null) {
                    if (icms.getICMSPart().getPICMS() != null) {
                        tributoIcms.setAliquota(Double.parseDouble(icms.getICMSPart().getPICMS()));
                    }
                    if (icms.getICMSPart().getVBC() != null) {
                        tributoIcms.setBaseCalculo(Double.parseDouble(icms.getICMSPart().getVBC()));
                    }
                    if (icms.getICMSPart().getVICMS() != null) {
                        tributoIcms.setValor(Double.parseDouble(icms.getICMSPart().getVICMS()));
                    }
                    tributoIcms.setCst(icms.getICMSPart().getCST());
                    tributoIcms.setNome("ICMS");
                    tributoIcms.setProduto(produto);
                    temIcms = true;
                } else if (icms.getICMSSN101() != null) {
                    if (icms.getICMSSN101().getPCredSN() != null) {
                        tributoIcms.setAliquota(Double.parseDouble(icms.getICMSSN101().getPCredSN()));
                    }
                    if (icms.getICMSSN101().getVCredICMSSN() != null) {
                        tributoIcms.setValor(Double.parseDouble(icms.getICMSSN101().getVCredICMSSN()));
                    }
                    tributoIcms.setCst(icms.getICMSSN101().getCSOSN());
                    tributoIcms.setNome("ICMS");
                    tributoIcms.setProduto(produto);
                    temIcms = true;
                } else if (icms.getICMSSN201() != null) {
                    if (icms.getICMSSN201().getPCredSN() != null) {
                        tributoIcms.setAliquota(Double.parseDouble(icms.getICMSSN201().getPCredSN()));
                    }
                    if (icms.getICMSSN201().getVCredICMSSN() != null) {
                        tributoIcms.setValor(Double.parseDouble(icms.getICMSSN201().getVCredICMSSN()));
                    }
                    tributoIcms.setCst(icms.getICMSSN201().getCSOSN());
                    tributoIcms.setNome("ICMS");
                    tributoIcms.setProduto(produto);
                    temIcms = true;
                } else if (icms.getICMSSN202() != null) {
                    if (icms.getICMSSN202().getPICMSST() != null) {
                        tributoIcms.setAliquota(Double.parseDouble(icms.getICMSSN202().getPICMSST()));
                    }
                    if (icms.getICMSSN202().getVBCST() != null) {
                        tributoIcms.setBaseCalculo(Double.parseDouble(icms.getICMSSN202().getVBCST()));
                    }
                    if (icms.getICMSSN202().getVICMSST() != null) {
                        tributoIcms.setValor(Double.parseDouble(icms.getICMSSN202().getVICMSST()));
                    }
                    tributoIcms.setCst(icms.getICMSSN202().getCSOSN());
                    tributoIcms.setNome("ICMS");
                    tributoIcms.setProduto(produto);
                    temIcms = true;
                } else if (icms.getICMSSN900() != null) {
                    if (icms.getICMSSN900().getPICMS() != null) {
                        tributoIcms.setAliquota(Double.parseDouble(icms.getICMSSN900().getPICMS()));
                    }
                    if (icms.getICMSSN900().getVBC() != null) {
                        tributoIcms.setBaseCalculo(Double.parseDouble(icms.getICMSSN900().getVBC()));
                    }
                    if (icms.getICMSSN900().getVICMS() != null) {
                        tributoIcms.setValor(Double.parseDouble(icms.getICMSSN900().getVICMS()));
                    }
                    tributoIcms.setCst(icms.getICMSSN900().getCSOSN());
                    tributoIcms.setNome("ICMS");
                    tributoIcms.setProduto(produto);
                    temIcms = true;
                } else if (icms.getICMSST() != null) {
                    if (icms.getICMSST().getVBCSTDest() != null) {
                        tributoIcms.setBaseCalculo(Double.parseDouble(icms.getICMSST().getVBCSTDest()));
                    }
                    if (icms.getICMSST().getVICMSSTDest() != null) {
                        tributoIcms.setValor(Double.parseDouble(icms.getICMSST().getVICMSSTDest()));
                    }
                    tributoIcms.setCst(icms.getICMSST().getCST());
                    tributoIcms.setNome("ICMS");
                    tributoIcms.setProduto(produto);
                    temIcms = true;
                }

                if (temIcms) {
                    em.persist(tributoIcms);
                }

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
