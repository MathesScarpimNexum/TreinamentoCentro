package com.lecom.workflow.integracao.pagina.GeradorPdf;

import static br.com.lecom.api.factory.ECMFactory.documento;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.stream.Collectors;

import javax.xml.bind.JAXBElement;

import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.docx4j.XmlUtils;
import org.docx4j.model.datastorage.migration.VariablePrepare;
import org.docx4j.openpackaging.packages.WordprocessingMLPackage;
import org.docx4j.openpackaging.parts.WordprocessingML.MainDocumentPart;
import org.docx4j.wml.ContentAccessor;
import org.docx4j.wml.P;
import org.docx4j.wml.Tbl;
import org.docx4j.wml.Tc;
import org.docx4j.wml.Text;
import org.docx4j.wml.Tr;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.lecom.workflow.cadastros.common.util.Funcoes;
import com.lecom.workflow.cadastros.rotas.DocumentosECM;
import com.lecom.workflow.cadastros.rotas.LoginAutenticacao;
import com.lecom.workflow.cadastros.rotas.exception.DocumentosECMException;
import com.lecom.workflow.cadastros.rotas.exception.LoginAuthenticationException;
import com.lecom.workflow.cadastros.rotas.util.DadosDocumento;
import com.lecom.workflow.cadastros.rotas.util.DadosLogin;
import com.lecom.workflow.robo.AlteraSlaBPM.RoboAlteraSlaBPM;
import com.lecom.workflow.vo.IntegracaoVO;

import br.com.docsys.ecm.client.dto.document.Document;
import br.com.docsys.ecm.client.dto.file.DocFile;
import br.com.lecom.atos.servicos.annotation.Execution;
import br.com.lecom.atos.servicos.annotation.IntegrationModule;
import br.com.lecom.atos.servicos.annotation.Version;
import fr.opensagres.poi.xwpf.converter.pdf.PdfConverter;
import fr.opensagres.poi.xwpf.converter.pdf.PdfOptions;

@IntegrationModule("GeradorPDFAlternativo")
@Version({1,1,10})
public class GeradorPDF {

	private static final Logger logger = LoggerFactory.getLogger(GeradorPDF.class);
	private String configPath = Funcoes.getWFRootDir() + "/upload/cadastros/pdfGerado";

	@SuppressWarnings("unchecked")
	@Execution
	public String geraPDF(IntegracaoVO integracaoVO) {
		logger.info("Inicio Gerador de PDF processo " + integracaoVO.getCodProcesso());
		try {
			
			Map<String, Object> camposModelo = integracaoVO.getMapCamposFormulario();
			// logger.debug("Campos = " + camposModelo);
			logger.debug("Nome Tabela Modelo = " + integracaoVO.getNomeTabelaModelo());

			String fileId = (String) camposModelo.get("$PDF_ENTRADA_ID"); // id Arquivo no ecm fid e id template
			String pdfSaida = (String) camposModelo.get("$PDF_SAIDA");
			// identificador do template de saida
			String identificadorTemplateSaida = (String) camposModelo.get("$PDF_TEMPLATE_SAIDA");
			String nomeArquivo = (String) camposModelo.get("$PDF_NOME");
			String codCampoSaida = (String) camposModelo.get("$PDF_CAMPO_ID");
			String camposGrid = (String) camposModelo.get("$PDF_GRIDS");

			logger.info("ID docx = " + fileId);
			logger.info("Identificador do template de saida = " + identificadorTemplateSaida);
			logger.info("Campo de saida = " + codCampoSaida);
			logger.info("Arquivo Final = " + pdfSaida);

			// verifica se o arquivo ja existe
			// (pdfSaida == null || pdfSaida.trim().equals(""))&& 
			if (!fileId.equals("")) {
				String[] dadosId = fileId.split(",");
				String uniqueIdECM = getUniquqIdECM(dadosId[0],dadosId[1]);
				logger.debug("Inicio leitura doc = " + uniqueIdECM);
				InputStream arquivoModelo = documento().lerArquivo(uniqueIdECM);

				logger.debug(" achei input = " + arquivoModelo);
				// Abre o documento
				WordprocessingMLPackage wordprocessingMLPackage = WordprocessingMLPackage.load(arquivoModelo);
				// Recupera a parte principal dele

				logger.debug("Recupera a parte principal dele");
				MainDocumentPart mainDocumentPart = wordprocessingMLPackage.getMainDocumentPart();

				HashMap<String, String> hashMap = montaHashMapTrocaVariavel(camposModelo, integracaoVO);

				// Efetua as trocas
				logger.debug("TROCAR VALORES");
				try {

					VariablePrepare.prepare(wordprocessingMLPackage);

					mainDocumentPart.variableReplace(hashMap);

					if (camposGrid != null && !camposGrid.equals("")) {
						if (camposGrid.contains(",")) {
							String[] identificadoresGrid = camposGrid.split(",");

							for (String identificador : identificadoresGrid) {
								geraInformacoesGrid(integracaoVO, wordprocessingMLPackage, identificador);
							}
						} else {
							geraInformacoesGrid(integracaoVO, wordprocessingMLPackage, camposGrid);
						}
					}

					logger.debug("GERAR NOVO ARQUIVO");
					// criando diretório
					new File(configPath).mkdirs();

					String arquivoDocGerado = configPath + "/" + nomeArquivo+"_"+integracaoVO.getCodProcesso() + ".docx";
					String arquivoPDFGerado = configPath + "/" + nomeArquivo+"_"+integracaoVO.getCodProcesso() + ".pdf";

					File destDocx = new File(arquivoDocGerado);
					wordprocessingMLPackage.save(destDocx);

					logger.debug("GERANDO PDF");
					File arquivoPDFDocx = new File(arquivoPDFGerado);

					try (FileInputStream is = new FileInputStream(destDocx)) {
						XWPFDocument docx = new XWPFDocument(is);
						PdfOptions options = PdfOptions.create();
						logger.debug("CRIANDO ARQUIVO BASEADO NO DOCX");
						try (OutputStream outputStreamDocx = new FileOutputStream(arquivoPDFDocx)) {
							PdfConverter.getInstance().convert(docx, outputStreamDocx, options);
						}
					}

					logger.debug("INSERINDO ARQUIVO NO ECM");
					// Recupera nome do arquivo e o nome criptografado
					Document documento = documento().criarDocumentoComIdentificador(arquivoPDFDocx, identificadorTemplateSaida).salvar();
					DocFile arquivoDoc = documento.getCurrentFile();
					String nomeArquivo1 = arquivoDoc.getFileName();
					String nomeCriptografado = arquivoDoc.getFileUniqueId().getValue();
					logger.debug("ARQUIVO GERADO : " + nomeArquivo1);
					logger.debug("NOME CRIPTOGRAFADO : " + nomeCriptografado);

					// Apaga arquivo da pasta pública do servidor
					arquivoPDFDocx.delete();
					destDocx.delete();

					if (!nomeCriptografado.equals("")) {
						relacionarProcessoTemplate(integracaoVO, integracaoVO.getCodProcesso(),
								integracaoVO.getCodEtapa(), integracaoVO.getCodCiclo(), nomeCriptografado,
								documento.getId().getValue(), codCampoSaida);
					}

					alteraValorCampoSaida(integracaoVO, nomeArquivo1 + ":" + nomeCriptografado);

					return "0|" + nomeArquivo1 + ":" + nomeCriptografado;

				} catch (Exception e) {
					logger.error("Erro ao gerar pdf ",e);
					e.printStackTrace();
					return "99|Não foi possível gerar PDF";
				}

			} else {
				if (fileId.equals("")) {
					return "99|Não existe numero do modelo";
				}
				/*
				 * if (!pdfSaida.equals("")) { return "0|" + pdfSaida; }
				 * logger.info("PDF JÁ EXISTE NO FORMULÁRIO");
				 */
			}
		} catch (Exception e) {
			logger.error("Erro ao gerar pdf ",e);
			
			e.printStackTrace();
		}
		return "99|Não foi possível gerar PDF";
	}

	//teste
	private void geraInformacoesGrid(IntegracaoVO integracaoVO, WordprocessingMLPackage wordprocessingMLPackage,
			String identificador) throws Exception {

		logger.info("geraInformacoesGrid identificador = " + identificador);

		String nomeTabelaGrid = integracaoVO.getNomeTabelaModelo().replaceAll("f_", "g_");
		String tabelaGrid = nomeTabelaGrid + identificador;
		logger.info("geraInformacoesGrid tabelaGrid = " + tabelaGrid);

		integracaoVO.setConexao("workflow");
		List<Map<String, Object>> gridData = new ArrayList<Map<String, Object>>();
		try (Connection con = integracaoVO.getConexao()) {
			StringBuilder strProesso = new StringBuilder();
			strProesso.append("select cod_form,cod_versao from processo p ");
			strProesso.append(" where cod_processo = ? ");
			String codForm = "";
			String codVersao = "";
			try (PreparedStatement pst = con.prepareStatement(strProesso.toString())) {
				pst.setString(1, integracaoVO.getCodProcesso());
				try (ResultSet rs = pst.executeQuery()) {
					if (rs.next()) {
						codForm = rs.getString("cod_form");
						codVersao = rs.getString("cod_versao");
					}
				}
			}

			StringBuilder strCamposGrid = new StringBuilder();
			strCamposGrid.append(
					"select DES_NOME from campo where cod_form = ? and cod_versao = ? and id_agrupamento_grid = ? ");
			List<String> nomesCamposGrid = new ArrayList<String>();
			try (PreparedStatement pst = con.prepareStatement(strCamposGrid.toString())) {
				pst.setString(1, codForm);
				pst.setString(2, codVersao);
				pst.setString(3, identificador);
				try (ResultSet rs = pst.executeQuery()) {
					while (rs.next()) {
						nomesCamposGrid.add(rs.getString("DES_NOME"));
					}
				}
			}

			StringBuilder dadosGrid = new StringBuilder();
			dadosGrid.append("select * from ");
			dadosGrid.append(tabelaGrid);
			dadosGrid.append(" where cod_processo = ? ");
			dadosGrid.append("and cod_etapa = ? ");
			dadosGrid.append("and cod_ciclo = ? ");
			try (PreparedStatement pst = con.prepareStatement(dadosGrid.toString())) {
				pst.setString(1, integracaoVO.getCodProcesso());
				pst.setString(2, integracaoVO.getCodEtapa());
				pst.setString(3, integracaoVO.getCodCiclo());

				try (ResultSet rs = pst.executeQuery()) {
					while (rs.next()) {
						Map<String, Object> mapCamposGrid = new HashMap<String, Object>();
						for (String nomeColuna : nomesCamposGrid) {
							mapCamposGrid.put(nomeColuna, rs.getObject(nomeColuna));
						}
						gridData.add(mapCamposGrid);
					}
				}

			}

			if (gridData.size() == 0) {
				Map<String, Object> mapCamposGrid = new HashMap<String, Object>();

				nomesCamposGrid.forEach(nomeColuna -> mapCamposGrid.put(nomeColuna, ""));

				gridData.add(mapCamposGrid);
			}
		}

		logger.info("geraInformacoesGrid gridData = " + gridData);
		List<Map<String, String>> listHashMap = gridData.stream()
				.map(map -> map.entrySet().stream()
						.collect(Collectors.toMap(this::formataChaveGrid, this::formataValorGrid)))
				.collect(Collectors.toList());
		logger.info("geraInformacoesGrid listHashMap = " + listHashMap);
		Set<String> mapFields = listHashMap.stream().findFirst().orElseGet(Collections::emptyMap).keySet();
		logger.info("geraInformacoesGrid mapFields = " + mapFields);

		atualizaTabela(mapFields.toArray(new String[mapFields.size()]), listHashMap, wordprocessingMLPackage);
	}

	public String formataChaveGrid(Entry<String, Object> entry) {
		String key = entry.getKey().replace("$", "");
		String keyret = "_" + key + "_";
		return keyret;
	}

	public String formataValorGrid(Entry<String, Object> entry) {
		Object valor = entry.getValue();
		if(Funcoes.nulo(valor, "").equals("")){
			return " ";
		}else{
			String retorno = converteData(valor);
			if(retorno.indexOf("&amp;") < 0){
				retorno = retorno.toUpperCase();
			}
			return retorno;
		}	  
		
	}

	private static String converteData(Object valor) {
		String valorRetorno = "";
		String formato = String.valueOf(valor).contains("T") ? "yyyy-MM-dd'T'HH:mm:ss.SSSX" : "yyyy-MM-dd HH:mm:ss.S";

		try {
			valorRetorno = new SimpleDateFormat("dd/MM/yyyy").format(new SimpleDateFormat(formato).parse(String.valueOf(valor)));
		} catch (ParseException e) {
			valorRetorno = String.valueOf(valor).toUpperCase();
			valorRetorno = convertString(valorRetorno);
		}
		return valorRetorno;
	}

	private static void atualizaTabela(String[] placeholders, List<Map<String, String>> textToAdd,
			WordprocessingMLPackage template) throws Exception {
		logger.debug("VALORES PARA ADICIONAR : " + textToAdd);
		if (placeholders.length > 0) {
			List<Object> tables = getAllElementFromObject(template.getMainDocumentPart(), Tbl.class);
			// Encontra tabela no doc
			Tbl tempTable = getTemplateTable(tables, placeholders);
			List<Object> rows = getAllElementFromObject(tempTable, Tr.class);

			// tabela precisa ter 2 linhas (cabeçalho e conteudo)
			if (rows.size() == 2) {
				Tr templateRow = (Tr) rows.get(1);

				// adiciona linhas a tabela
				textToAdd.forEach(replacements -> addRowToTable(tempTable, templateRow, replacements));

				// Remove linha
				tempTable.getContent().remove(templateRow);
			}
		}
	}

	private static List<Object> getAllElementFromObject(Object obj, Class<?> toSearch) {
		List<Object> result = new ArrayList<Object>();
		if (obj instanceof JAXBElement)
			obj = ((JAXBElement<?>) obj).getValue();

		if (obj != null && obj.getClass().equals(toSearch))
			result.add(obj);
		else if (obj instanceof ContentAccessor) {
			List<?> children = ((ContentAccessor) obj).getContent();
			children.forEach(child -> result.addAll(getAllElementFromObject(child, toSearch)));
		}
		return result;
	}

	private static Tbl getTemplateTable(List<Object> tables, String[] templateKey) throws Exception {
		for (Iterator<Object> iterator = tables.iterator(); iterator.hasNext();) {
			Object tbl = iterator.next();
			// Pega todas as células da tabela
			List<?> cells = getAllElementFromObject(tbl, Tc.class);
			for (Object cell : cells) {
				// Pega todos os parágrafos da célula
				List<?> paragraphs = getAllElementFromObject((Tc) cell, P.class);

				for (Object paragraph : paragraphs) {
					// Pega todos os textos do parágrafo
					List<?> texts = getAllElementFromObject((P) paragraph, Text.class);

					int i = 0;
					for (Object text : texts) {
						// Somente executa do segundo texto em diante
						if (i > 0) {
							// Move o conteúdo do texto para o primeiro texto, ao final (concatena)
							((Text) texts.get(0)).setValue(((Text) texts.get(0)).getValue() + ((Text) text).getValue());
							// Limpa o conteúdo do texto, para efetuar o recorte
							((Text) text).setValue("");
						}
						i++;
					}

					for (Object text : texts) {
						Text textElement = (Text) text;
						// Verifica se textElement existe e se existe uma chave para o elemento
						if (textElement.getValue() != null
								&& Arrays.asList(templateKey).contains(textElement.getValue()))
							return (Tbl) tbl;
					}
				}
			}
		}
		return null;
	}

	private static void addRowToTable(Tbl reviewtable, Tr templateRow, Map<String, String> replacements) {
		Tr workingRow = (Tr) XmlUtils.deepCopy(templateRow);
		// Pega todas as células da tabela
		List<?> cells = getAllElementFromObject(workingRow, Tc.class);

		for (Object cell : cells) {
			// Pega todos os parágrafos da célula
			List<?> paragraphs = getAllElementFromObject((Tc) cell, P.class);

			for (Object paragraph : paragraphs) {
				// Pega todos os textos do parágrafo
				List<?> texts = getAllElementFromObject((P) paragraph, Text.class);

				int i = 0;
				for (Object text : texts) {
					if (i > 0) {
						// Move o conteúdo do texto para o primeiro texto, ao final (concatena)
						((Text) texts.get(0)).setValue(((Text) texts.get(0)).getValue() + ((Text) text).getValue());
						// Limpa o conteúdo do texto, para efetuar o recorte
						((Text) text).setValue("");
					}
					i++;
				}

				Text text = (Text) texts.get(0);
				String replacementValue = (String) replacements.get(text.getValue());
				if (replacementValue != null) {
					text.setValue(replacementValue);
				}
			}
		}
		reviewtable.getContent().add(workingRow);
	}

	public void alteraValorCampoSaida(IntegracaoVO integracaoVO, String valorCampoPDF) throws Exception {
		integracaoVO.setConexao("workflow");
		try (Connection con = integracaoVO.getConexao()) {
			String update = "update " + integracaoVO.getNomeTabelaModelo()
					+ " set PDF_SAIDA = ? where cod_processo_f = ? and cod_etapa_f = ? and cod_ciclo_f = ? ";
			try (PreparedStatement pst = con.prepareStatement(update)) {
				pst.setString(1, valorCampoPDF);
				pst.setString(2, integracaoVO.getCodProcesso());
				pst.setString(3, integracaoVO.getCodEtapa());
				pst.setString(4, integracaoVO.getCodCiclo());
				pst.executeUpdate();
			}

		}
	}

	public void relacionarProcessoTemplate(IntegracaoVO integracaoVO, String codProcesso, String codEtapa,
			String codCiclo, String uniqueId, Long idDocumento, String codCampoPDfGerado) throws Exception {
		try (Connection con = integracaoVO.getConexao()) {
			String selectTemplate = "select count(*) as total from processo_template where cod_processo = ?  and cod_etapa = ? and cod_ciclo = ? and cod_campo = ?";
			try (PreparedStatement pst = con.prepareStatement(selectTemplate)) {
				pst.setString(1, codProcesso);
				pst.setString(2, codEtapa);
				pst.setString(3, codCiclo);
				pst.setString(4, codCampoPDfGerado);
				logger.debug(" codProcesso = "+codProcesso+" codEtapa = "+codEtapa+" codCiclo= "+codCiclo+" codCampoPDfGerado = "+codCampoPDfGerado);
				try (ResultSet rs = pst.executeQuery()) {
					if (rs.next()) {
						int total = rs.getInt("total");
						String sqlUpdateProcessoTemplate = (total > 0)
								? "update processo_template set cod_arquivo_ecm = ?, cod_documento_ecm = ? where cod_processo= ? and cod_etapa = ? and cod_ciclo = ? and cod_campo = ?"
								: "insert into processo_template (cod_arquivo_ecm,cod_documento_ecm,cod_processo,cod_etapa,cod_ciclo,cod_campo) values (?,?,?,?,?,?)";
						logger.debug(" sqlUpdateProcessoTemplate = "+sqlUpdateProcessoTemplate);
						logger.debug(" uniqueId = "+uniqueId+" idDocumento = "+idDocumento+" codProcesso = "+codProcesso+" codEtapa = "+codEtapa);
						logger.debug(" codCiclo= "+codCiclo+" codCampoPDfGerado = "+codCampoPDfGerado);
						try (PreparedStatement pstUpdateProcessoTemplate = con.prepareStatement(sqlUpdateProcessoTemplate)) {
							pstUpdateProcessoTemplate.setString(1, uniqueId);
							pstUpdateProcessoTemplate.setLong(2, idDocumento);
							pstUpdateProcessoTemplate.setString(3, codProcesso);
							pstUpdateProcessoTemplate.setString(4, codEtapa);
							pstUpdateProcessoTemplate.setString(5, codCiclo);
							pstUpdateProcessoTemplate.setString(6, codCampoPDfGerado);
							
							boolean retorno = pstUpdateProcessoTemplate.execute();
							logger.debug("retorno = "+retorno);
						}
					}
				}
			}
		}
	}

	public String getUniquqIdECM(String fileId, String idTemplate){
		logger.debug("getUniquqIdECM fileId = "+fileId+" idTempalte = "+idTemplate );
		DadosDocumento info = null;
		try {
			DadosLogin dadosLogin = new DadosLogin("adm", "lecomcentro", false);
			LoginAutenticacao login = new LoginAutenticacao("https://bpm-hml.sicoobcentro.com.br/sso", dadosLogin);
			logger.debug("login = "+login.getToken());
			logger.debug(idTemplate);
			DocumentosECM documentosEcm = new DocumentosECM("https://bpm-hml.sicoobcentro.com.br/ecmcore", login.getToken(), idTemplate, "0", 50);
			List<DadosDocumento> dados;
			dados = documentosEcm.getDocumentosByTemplate();
			logger.debug("dados = "+dados.size());
			info = dados.stream().filter(x -> fileId.equals(x.getDocumentId())).findAny().get();
			logger.debug("UNIQUE ID ECM " +  info.getFileUniqueId() + " ");
		} catch (DocumentosECMException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (LoginAuthenticationException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		return info.getFileUniqueId();
	
	}
	
	

	public HashMap<String, String> montaHashMapTrocaVariavel(Map<String, Object> camposModelo,
			IntegracaoVO integracaoVO) throws Exception {
		HashMap<String, String> variaveisReplace = new HashMap<String, String>();
		camposModelo.forEach((k, v) -> {
			String key = k.replace("$", "");
			String chaveNova = key;
			variaveisReplace.put(chaveNova, formataValor(v));
		});

		// preenchendo alguns padroes
		variaveisReplace.put("PROCESSO", integracaoVO.getCodProcesso());
		variaveisReplace.put("USER_INICIADOR", getNomeUsuario(integracaoVO.getCodUsuarioIniciador(), integracaoVO));
		variaveisReplace.put("USER_RESPONSAVEL", getNomeUsuario(integracaoVO.getCodUsuarioEtapa(), integracaoVO));
		variaveisReplace.put("DIA", String.valueOf(LocalDate.now().getDayOfMonth()));
		variaveisReplace.put("MES", String.valueOf(LocalDate.now().getMonthValue()));
		Calendar hoje = Calendar.getInstance();
		String mesExtenso = new SimpleDateFormat("MMMM").format(hoje.getTime());

		variaveisReplace.put("MES_EXTENSO", mesExtenso);
		variaveisReplace.put("ANO", String.valueOf(LocalDate.now().getYear()));

		logger.debug("CAMPOS DO FORMULARIO : " + variaveisReplace);

		return variaveisReplace;
	}

	public static String formataValor(Object entry) {
		Object valor = entry;
		if(Funcoes.nulo(valor, "").equals("")){
			return " ";
		}else{
			String retorno = converteData(valor);
			if(retorno.indexOf("&amp;") < 0){
				retorno = retorno.toUpperCase();
			}
			return retorno;
		}	   
	}

	public static String convertString(String valor) {
		logger.debug("VALOR PARA CONVERTER : " + valor);

		valor = (valor.trim().indexOf("&") > 0) ? valor.replaceAll("&(?!amp;)", "&amp;") : valor;

		logger.debug("VALOR CONVERTIDO : " + valor);

		return valor;
	}

	public String getNomeUsuario(String codUserIniciador, IntegracaoVO integracaoVO) throws Exception {
		integracaoVO.setConexao("workflow");
		String nomeUsuario = "";
		try (Connection con = integracaoVO.getConexao()) {
			String sql = "select nom_usuario from usuario where cod_usuario = ?";
			try (PreparedStatement pst = con.prepareStatement(sql)) {
				pst.setString(1, codUserIniciador);
				try (ResultSet rs = pst.executeQuery()) {
					if (rs.next()) {
						nomeUsuario = rs.getString("nom_usuario");
					}
				}
			}
		}
		return nomeUsuario;
	}
}
