package com.lecom.workflow.integracoes.generico;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.lecom.tecnologia.db.DBUtils;
import com.lecom.workflow.robo.satelite.WFMail;
import com.lecom.workflow.vo.IntegracaoVO;

import br.com.lecom.atos.servicos.annotation.Execution;
import br.com.lecom.atos.servicos.annotation.IntegrationModule;
import br.com.lecom.atos.servicos.annotation.Version;
import br.com.lecom.workflow.email.EmailMessage;

/**
 * Integra��o para disparo de e-mail generica utilizando json do campo do
 * processo.
 * 
 * -Dependencia Criacao do campo AUX_EMAIL no processo uso do json no seguinte
 * formato { "enderecoEmailEnvio": { "grupos": [], "emails": [] }, "dados": {
 * "assunto": "", "camposFormulario": [], "grids": [ "grids"{nome da grid:[nome dos campos da grid]}  },
 * 
 * "templateHtml": "",
 * 
 * }
 * 
 * 
 * exemplo:
 * 
 * 
 * 	{
	    "enderecoEmailEnvio": {
	        "grupos": [],
	        "emails": ["teste@gmail.com"]
	    },
	    "dados": {
	        "assunto": "Teste E-mail",
	        "camposFormulario": ["NOME", "RB_ENVIAR_EMAIL"],
	        "grids": {
  					"GRID2": [
	                "CAMPO3"
	            ]
	            "GRID1": [
	                "CAMPO1",
	                "CAMPO2"
	            ],
	          

	        }
	    },

	    "templateHtml": "",

	}
 * 
 * 
 * 
 * 
 * 
 * Descri��o dos atributos: grupos: Arrays com o nome dos grupos que os usu�rios
 * dever�o receber e-mail. Neste caso o sistema ir� identificar automaticamente
 * os e-mails de todos os usu�rios ativos no grupos email: endere�os de e-mail
 * que dever�o receber o email. Aten��o � obrigat�rio o preenchimento do grupos
 * ou e-mails. assunto: assunto que ser� enviado no e-mail. camposFormulario: o
 * nome dos campos que dever�o ser enviados no processo, � considerado aqui o
 * valor do campo da etapa em andamento. templateHtml: html personalizado que
 * ser� encaminhado no e-mail. Aten��o: caso o atributo templateHtml for
 * preenchido o atributo camposFormulario ser� desconsiderado.
 * 
 * @since 12/2021
 * @author Thiago.Costa
 * 
 *         adicionado possibilidade de recebimento de dados de grid via
 *         parametro json
 * 
 * @since 12/2021
 * @author Thiago Costa
 *
 */

@IntegrationModule("DisparoEmailGeneretico")
@Version({ 1, 0, 2 })
public class DisparoEmail {

	private static Logger logger = LoggerFactory.getLogger(DisparoEmail.class);

	@Execution
	public String start(IntegracaoVO integracaoVO) {

		logger.info(new String(new char[100]).replace("\0", "#"));
		logger.info("[==== INICIO DisparoEmailGeneretico ====]");

		@SuppressWarnings("unchecked")
		Map<String, String> camposFormulario = integracaoVO.getMapCamposFormulario();

		String auxParamsEmail = nulo(camposFormulario.get("$AUX_EMAIL"), "");

		if (auxParamsEmail.equals("")) {
			return "0|N�o h� e-mail cadastrados para envio!";
		}

		try {
			return enviarEmail(integracaoVO, auxParamsEmail);
		} catch (NumberFormatException e) {
			logger.error(e.toString());
			return "99|Erro ao enviar e-mail.";

		} catch (Exception e) {
			logger.error(e.toString());
			return "99|Erro ao enviar e-mail.";
		}

	}

	public String enviarEmail(IntegracaoVO integracaoVO, String auxParamsEmail)
			throws NumberFormatException, Exception {
		JSONObject paramEmail = toJSON(auxParamsEmail);
		int codProcesso = Integer.parseInt(integracaoVO.getCodProcesso());
		int codEtapa = Integer.parseInt(integracaoVO.getCodEtapa());
		int codCiclo = Integer.parseInt(integracaoVO.getCodCiclo());
		int codForm = Integer.parseInt(integracaoVO.getCodForm());
		String bodyEmail = null;
		if (paramsEndEmailError(paramEmail)) {
			return "99|Informe pelo menos um endere�o de e-mail para continuar.";
		}
		if (paramsDadosEmailError(paramEmail)) {
			return "99|Informe pelo menos um endere�o de e-mail para continuar.";
		}

		String from = integracaoVO.getDesFrom();

		JSONObject enderecoEmail = (JSONObject) paramEmail.get("enderecoEmailEnvio");
		JSONObject dadosEmail = (JSONObject) paramEmail.get("dados");
		JSONArray arrGrupos = (JSONArray) enderecoEmail.get("grupos");
		JSONArray arrEmails = (JSONArray) enderecoEmail.get("emails");
		StringBuilder strHtmlGrupo = new StringBuilder();
		ArrayList<String> arrEmailsGrupos = getEmailUsuarioGrupo(arrGrupos);
		final List<String> listEmailEnvio = mergeEmailEnvio(arrEmails, arrEmailsGrupos);
		final String assunto = nulo(dadosEmail.get("assunto"), "");

		logger.trace(new String(new char[100]).replace("\0", "#"));
		logger.trace("[ENDERECOS DE E-MAIL A SEREM ENVIADOS]");
		logger.trace(listEmailEnvio.toString());
		logger.trace(new String(new char[100]).replace("\0", "#"));

		// Caso exista um template html ser� enviado o template
		if (paramEmail.containsKey("templateHtml") && !paramEmail.get("templateHtml").equals("")) {
			bodyEmail = nulo(paramEmail.get("templateHtml"), "");
		} else if (dadosEmail.containsKey("camposFormulario") && !dadosEmail.get("camposFormulario").equals("")) {
			logger.trace("MONTANDO OBJ COM CAMPOS PARAMETRIZADOS");
			JSONArray camposFormulario = (JSONArray) dadosEmail.get("camposFormulario");
			JSONObject grids = null;
			ArrayList<Map<String, String>> dadosCampos = getInfoCampos(codProcesso, codEtapa, codCiclo, codForm,
					camposFormulario, integracaoVO);
			Map<String, String> mapUsuarioIniciador = getDadosUsuario(
					Integer.parseInt(integracaoVO.getCodUsuarioIniciador()));
			String nomeModelo = consultarNomeModelo(codProcesso);

			// Montando dados grid
			if (dadosEmail.containsKey("grids") && !dadosEmail.get("grids").equals("")) {
				logger.info("MONTANDO OBJ COM GRIDS");
				grids = (JSONObject) dadosEmail.get("grids");
				HashMap<String, String> mapCampos  = getDadosCampo(codProcesso, codEtapa, codCiclo,  codForm);
				strHtmlGrupo = montarStringhtmlGrupo(grids, integracaoVO,mapCampos);
				
			}
			logger.info("MONTANDO BODY DO E-MAIL");
			bodyEmail = htmlEmailPadrao(dadosCampos, strHtmlGrupo, codProcesso, nomeModelo,
					nulo(mapUsuarioIniciador.get("NOM_USUARIO"), ""));
			logger.trace("============================TEMPLATE HTML=================================================");
			 logger.trace(bodyEmail);
			logger.trace(
					"============================END TEMPLATE HTML=================================================");

		} else {
			return "99|Informe um template ou os campos para envio de e-mail";
		}

		EmailMessage emailMessage = new EmailMessage(assunto, bodyEmail, from, listEmailEnvio, true);
		WFMail wfmail = new WFMail();
		String ret = wfmail.enviaEmailMessage(emailMessage);
		if ("Falha ao enviar o e-mail.".equals(ret)) {
			return "99|" + ret;
		}
		logger.trace("Retorno envio e-mail " + ret);
		logger.info("[==== FIM  ====]");
		logger.info(new String(new char[100]).replace("\0", "#"));
		return "0|Email(s), enviados!";

	}

	private StringBuilder montarStringhtmlGrupo(JSONObject obGrids, IntegracaoVO integracaoVO, HashMap<String, String> dadosCampos) throws Exception {
			StringBuilder html = new StringBuilder();
			logger.info(dadosCampos.toString());
			for (Object nomeGrid : obGrids.keySet()) {
				ArrayList<String> listCamposGrid = getInFieldsByGrids(obGrids, nomeGrid.toString());
				html.append(" <table width='650' border='0' align='center' cellpadding='0' cellspacing='0'  style=' font-size: 13px; color: #9c9c9c;'> 			");
				html.append(" 	 	<thead> 			");
				for(String campo : listCamposGrid) {
					html.append("<th>"+dadosCampos.get(campo)+"</th>");
				}
				html.append(" 	 	</thead> 			");
				
				
				html.append(" 	 <tbody> 			");
				for (Map<String, Object> ob : integracaoVO.getDadosModeloGrid(nomeGrid.toString())) {
					html.append(" 	 <tr> 			");
					for(String campo : listCamposGrid) {
						html.append("<td align='left'>"+ob.get(campo)+"</td>");
					}
					html.append(" 	 </tr> 			");
				}
				html.append(" 	</tbody> 		");
				html.append(" </table> 			");
				html.append(" <br> 			");
			
		}
		return html;
	}

	
	private boolean paramsDadosEmailError(JSONObject paramEmail) {
		return !paramEmail.containsKey("dados");
	}

	private boolean paramsEndEmailError(JSONObject paramEmail) {
		JSONObject enderecoEmail = null;
		JSONArray arrGrupos = null;
		JSONArray arrEmails = null;

		if (!paramEmail.containsKey("enderecoEmailEnvio"))
			return true;

		enderecoEmail = (JSONObject) paramEmail.get("enderecoEmailEnvio");

		if (!enderecoEmail.containsKey("grupos") && !enderecoEmail.containsKey("emails"))
			return true;

		arrGrupos = (JSONArray) enderecoEmail.get("grupos");
		arrEmails = (JSONArray) enderecoEmail.get("emails");

		return arrGrupos.isEmpty() && arrEmails.isEmpty();

	}

	/**
	 * buscar o nome do modelo atrav�s do cod do processo.
	 * 
	 * @param cnWF
	 * @param logger
	 * @param codProcesso
	 * @return
	 */

	public static String consultarNomeModelo(int codProcesso) {
		String retorno = "";
		try {
			StringBuilder sSQL = new StringBuilder();
			sSQL.append(" SELECT f.DES_TITULO FROM FORMULARIO f ");
			sSQL.append("	INNER JOIN processo p ");
			sSQL.append("  ON f.COD_FORM = p.COD_FORM ");
			sSQL.append(" WHERE p.COD_PROCESSO = ? ");
			sSQL.append(" LIMIT 1 ");
			try (Connection cnWF = DBUtils.getConnection("workflow")) {
				try (PreparedStatement pst = cnWF.prepareStatement(sSQL.toString());) {
					pst.setInt(1, codProcesso);
					try (ResultSet rs = pst.executeQuery();) {
						if (rs.next()) {
							retorno = rs.getString("DES_TITULO");
						}
					}
				}
			}
		} catch (Exception e) {
			logger.error("[ERRO] Funcoes.consultarNomeModelo : ", e);

		}

		return retorno;
	}

	/**
	 * Metodo que le tabela de usuarios do workflow e retorna um mapa com as
	 * informacoes, com as chaves do Map sendo o nome das colunas da tabela
	 * 
	 * @param connection
	 * @param codUsuario
	 * @return
	 * @throws Exception
	 */
	public static Map<String, String> getDadosUsuario(Integer codUsuario) throws Exception {

		Map<String, String> parametroMap = new HashMap<>();

		String sql = " SELECT * " + " FROM USUARIO " + " WHERE COD_USUARIO = ? ";
		try (Connection connection = DBUtils.getConnection("workflow")) {
			try (PreparedStatement pst = connection.prepareStatement(sql);) {
				pst.setInt(1, codUsuario);

				try (ResultSet rs = pst.executeQuery();) {

					ResultSetMetaData metaData = rs.getMetaData();
					int columnCount = metaData.getColumnCount();

					if (rs.next()) {
						int columnIndex = 1;
						while (columnIndex < (columnCount + 1)) {
							parametroMap.put(metaData.getColumnName(columnIndex).toUpperCase(),
									nulo(rs.getString(columnIndex), ""));
							columnIndex++;
						}
					}
				}
				return parametroMap;
			}
		}
	}

	public static String htmlEmailPadrao(List<Map<String, String>> campos, StringBuilder htmlGrupo, int codProcesso,
			String nomeModelo, String solicitante) throws ParseException {
		StringBuilder html = new StringBuilder();
		html.append(" <html> ");
		html.append(" 		<head> ");
		html.append(" 		<title>Connect - " + nomeModelo + "</title> ");
		html.append(" 		<link rel='important stylesheet' href='chrome://messagebody/skin/messageBody.css'> ");
		html.append(" 		</head> ");
		html.append(" 		<body> ");
		html.append(
				" 		<table width='100%' border='0' align='center' cellpadding='0' cellspacing='0' style='font-family:Arial, Helvetica, sans-serif;'> ");
		html.append(" 		    <tbody> ");
		html.append(" 		        <tr> ");
		html.append(" 		            <td style='padding: 30px 10px;'>                                 ");
		html.append(
				" 		                <table width='650' border='0' align='center' cellpadding='0' cellspacing='0' style='font-family: Arial, Helvetica, sans-serif;' bgcolor='#ffffff'> ");
		html.append(" 		                    <tbody> ");
		html.append(" 		                        <tr> ");
		html.append(" 		                            <td style='padding: 8px 14px;' bgcolor='#1976D2'> ");
		html.append(
				" 		                                <table width='650' border='0' align='center' cellpadding='0' cellspacing='0'> ");
		html.append(" 		                                    <tbody>   ");
		html.append(" 		                                        <tr height='48'> ");
		html.append(
				" 		                                            <td width='325' align='left'><font style='font-size: 20px; color: #fff;'><strong>Connect</strong></font></td> ");
		html.append(
				" 		                                            <td width='325' align='right'><font style='font-size: 15px; color: #a9ccee;'>Plataforma Lecom</font></td> ");
		html.append(" 		                                        </tr>                                       ");
		html.append(" 		                                    </tbody> ");
		html.append(" 		                                </table> ");
		html.append(" 		                            </td> ");
		html.append(" 		                        </tr> ");
		html.append(" 		                        <tr height='30'><td></td></tr> ");
		html.append(" 		                        <tr> ");
		html.append(" 		                            <td> ");
		html.append(
				" 		                                <table width='650' border='0' align='center' cellpadding='0' cellspacing='0'> ");
		html.append(" 		                                    <tbody>   ");
		html.append(" 		                                        <tr> ");
		html.append(" 		                                            <td> ");
		html.append(
				" 		                                                <table width='650' border='0' align='center' cellpadding='0' cellspacing='0'> ");
		html.append(" 		                                                    <tbody>   ");
		html.append(" 		                                                        <tr> ");
		html.append(" 		                                                            <td> ");
		html.append(
				" 		                                                                <font style='color: #1976D2; font-size: 18px; '> ");
		html.append(" 		                                                                    <strong>" + nomeModelo
				+ "</strong> ");
		html.append(" 		                                                                </font> ");
		html.append("                                                                    </td> ");
		html.append("                                                                 </tr>   ");
		html.append("                                                                 <tr> ");
		html.append("                                                                     <td> ");
		html.append(
				"                                                                         <font style='color: #1976D2; font-size: 14px;'> ");
		html.append(
				"                                                                             <strong>Processo: </strong>"
						+ codProcesso);
		html.append("                                                                         </font> ");
		html.append("                                                                     </td> ");
		html.append("                                                                 </tr> ");
		html.append(" 		                                                        <tr height='10'><td></td></tr> ");
		html.append(" 		                                                        <tr> ");
		html.append(" 		                                                            <td> ");
		html.append(
				" 		                                                                <font style='color: #1976D2; font-size: 13px;'> ");
		html.append(" 			                                                                Solicitado por: "
				+ solicitante);
		html.append(" 		                                                                </font> ");
		html.append(" 		                                                            </td> ");
		html.append(
				" 		                                                        </tr>                                                         ");
		html.append(" 		                                                    </tbody> ");
		html.append(" 		                                                </table> ");
		html.append(" 		                                            </td> ");
		html.append(
				" 		                                        </tr>                                                    ");
		html.append(
				" 		                                        <tr height='10'><td></td></tr>                                  ");
		html.append(" 		                                        <tr><td height='2' bgcolor='#1976D2'></td></tr> ");
		html.append(
				" 		                                        <tr height='10'><td></td></tr>                                  ");
		html.append(" 		                                        <tr> ");
		html.append(" 		                                            <td> ");
		html.append(
				" 		                                                <table width='650' border='0' align='center' cellpadding='0' cellspacing='0'> ");
		html.append("	<tr height='30'>");
		html.append("	     <td>");
		html.append("	         <table width='650' border='0' align='center' cellpadding='0' cellspacing='0'>");
		campos.forEach(campo -> {
			if (campo.get("IDE_TIPO").equalsIgnoreCase("E")) {
				html.append("													<tbody>  ");
				html.append("                                                            ");
				html.append(
						"	<tr height='30'><td style='padding: 10px;'><font style='font-size: 13px; color: #1976D2;'><strong>"
								+ campo.get("LABEL") + "</strong></font></td></tr>");
				html.append("	<tr><td height='1' bgcolor='#e0e0e0'></td></tr>");
				html.append("                                                    </tbody>");
			} else {
				html.append("                                                    <tbody>  ");
				html.append("	<tr height='30'>");
				html.append("	     <td>");
				html.append(
						"	         <table width='650' border='0' align='center' cellpadding='0' cellspacing='0'>");
				html.append("	             <tbody>     ");
				html.append("	                 <tr>");
				html.append(
						"	                     <td width='200' align='left' style='padding: 10px;'><font style='font-size: 13px; color: #9c9c9c;'>"
								+ campo.get("LABEL") + ":</font></td>");
				html.append("	                     <td width='10'></td>");
				html.append(
						"	                     <td width='440' align='left' style='padding: 10px;'><font style='font-size: 13px; color: #3a3a3a;'>"
								+ campo.get("VALOR") + "</font></td>");
				html.append("	                 </tr>");
				html.append(
						"	             </tbody>                                                                    ");
				html.append("	         </table>");
				html.append("	     </td>");
				html.append("	 </tr>");
				html.append("	 <tr><td height='1' bgcolor='#e0e0e0'></td></tr>");
				html.append("                                                    </tbody>");
			}
		});

		html.append(" 		 ");
		html.append(" 		 ");
		html.append(" 		                                                </table> ");
		html.append(" 		                                            </td> ");
		html.append(" 		                                        </tr>        ");
		html.append(
				" 		                                        <tr height='10'><td></td></tr>                                      ");
		html.append(" 		                                    </tbody> ");
		html.append(" 		                                </table> ");
		
		if (htmlGrupo != null && !htmlGrupo.toString().equals("")) {
			html.append(htmlGrupo.toString());
		}

		html.append(" 		                            </td> ");
		html.append(" 		                        </tr>   ");
		html.append(" 		                        <tr height='30'><td></td></tr>                   ");
		html.append(" 		                        <tr> ");
		html.append(" 		                            <td bgcolor='#EEEEEE' style='padding: 8px 14px;'> ");
		html.append(
				" 		                                <table width='650' border='0' align='center' cellpadding='0' cellspacing='0'> ");
		html.append(" 		                                    <tbody>   ");
		html.append(" 		                                        <tr height='48'> ");
		html.append(
				" 		                                            <td width='325' align='left'><font style='font-size: 13px; color: #9c9c9c;'>Uso autorizado para <strong>Lecom S/A</strong></font></td> ");
		html.append(
				" 		                                            <td width='325' align='right'><font style='font-size: 13px; color: #9c9c9c;'>Desenvolvido por <strong>Lecom S.A.</strong></font></td> ");
		html.append(" 		                                        </tr>                                       ");
		html.append(" 		                                    </tbody> ");
		html.append(" 		                                </table> ");
		html.append(" 		                            </td> ");
		html.append(" 		                        </tr>                     ");
		html.append(" 		                    </tbody> ");
		html.append(" 		                </table> ");
		html.append(" 		            </td> ");
		html.append(" 		        </tr> ");
		html.append(" 		    </tbody> ");
		html.append(" 		</table></body> ");
		html.append(" 		</html> ");
		return html.toString();
	}

	@SuppressWarnings("unchecked")
	private ArrayList<Map<String, String>> getInfoCampos(int codProcesso, int codEtapa, int codCiclo, int codForm,
			JSONArray camposFormulario, IntegracaoVO integracaoVO) throws SQLException {

		ArrayList<Map<String, String>> dados = new ArrayList<>();

		StringBuilder query = new StringBuilder();
		query.append("SELECT ");
		query.append("CP.DES_LABEL, ");
		query.append("CP.IDE_TIPO ");
		query.append("FROM ");
		query.append("CAMPO CP ");
		query.append("INNER JOIN PROCESSO PR ");
		query.append("ON CP.COD_FORM = PR.COD_FORM ");
		query.append("AND CP.COD_VERSAO = PR.COD_VERSAO ");
		query.append("WHERE PR.COD_PROCESSO = ? ");
		query.append("AND PR.COD_ETAPA_ATUAL = ? ");
		query.append("AND PR.COD_CICLO_ATUAL = ? ");
		query.append("AND CP.DES_NOME = ? ");
		query.append("AND CP.COD_FORM = ?");
		try (Connection con = DBUtils.getConnection("workflow")) {
			camposFormulario.forEach(campo -> {
				try (PreparedStatement pst = con.prepareStatement(query.toString())) {
					int index = 1;
					pst.setInt(index++, codProcesso);
					pst.setInt(index++, codEtapa);
					pst.setInt(index++, codCiclo);
					pst.setString(index++, campo.toString());
					pst.setInt(index++, codForm);
					try (ResultSet rs = pst.executeQuery()) {
						while (rs.next()) {

							Map<String, String> dado = new HashMap<>();

							if (rs.getString("IDE_TIPO").equalsIgnoreCase("E")) {
								dado.put("IDE_TIPO", rs.getString("IDE_TIPO"));
								dado.put("LABEL", rs.getString("DES_LABEL"));
							} else {
								dado.put("IDE_TIPO", rs.getString("IDE_TIPO"));
								dado.put("LABEL", rs.getString("DES_LABEL"));
								dado.put("VALOR", nulo(integracaoVO.getMapCamposFormulario().get("$" + campo), ""));
							}

							dados.add(dado);
						}
					}
				} catch (Exception e) {
					logger.error("Erro ao buscar campos: " + e);
				}
			});

			return dados;
		}
	}
	

	private HashMap<String, String> getDadosCampo(int codProcesso, int codEtapa, int codCiclo, int codForm)
			throws SQLException {
		HashMap<String, String> mapCampos = new HashMap<String, String>();
		StringBuilder query = new StringBuilder();
		query.append("SELECT ");
		query.append("CP.DES_LABEL, ");
		query.append("CP.DES_NOME  ");
		query.append("FROM ");
		query.append("CAMPO CP ");
		query.append("INNER JOIN PROCESSO PR ");
		query.append("ON CP.COD_FORM = PR.COD_FORM ");
		query.append("AND CP.COD_VERSAO = PR.COD_VERSAO ");
		query.append("WHERE PR.COD_PROCESSO = ? ");
		query.append("AND PR.COD_ETAPA_ATUAL = ? ");
		query.append("AND PR.COD_CICLO_ATUAL = ? ");
		query.append("AND CP.COD_FORM = ? ");

		try (Connection con = DBUtils.getConnection("workflow")) {
			try (PreparedStatement pst = con.prepareStatement(query.toString())) {
				int index = 1;
				pst.setInt(index++, codProcesso);
				pst.setInt(index++, codEtapa);
				pst.setInt(index++, codCiclo);
				pst.setInt(index++, codForm);
				try (ResultSet rs = pst.executeQuery()) {
					while (rs.next()) {
						logger.info("eita");
						mapCampos.put(rs.getString("DES_NOME"), rs.getString("DES_LABEL"));
					}

				} catch (Exception e) {
					logger.error("Erro ao buscar campos: " + e);
				}
			}
		}
		return mapCampos;
	}

	private static ArrayList<String> getInFieldsByGrids(JSONObject grids, String nomeGrid) throws ParseException {
		ArrayList<String>campos = new ArrayList<String>();
		for (Object ob : toJSONArr(grids.get(nomeGrid).toString())) {
			campos.add(ob.toString());
		}
		
		
		return campos;
	}
	/**
	 * merge endere�os de e-mail
	 * 
	 * @param arrEmails
	 * @param emailUsuGrupos
	 * @return
	 */
	@SuppressWarnings("unchecked")
	private static List<String> mergeEmailEnvio(JSONArray arrEmails, ArrayList<String> emailUsuGrupos) {
		List<String> email = new ArrayList<>();
		if (emailUsuGrupos != null) {
			emailUsuGrupos.forEach(email::add);
		}
		if (arrEmails != null) {
			arrEmails.forEach(emailUsu -> email.add(emailUsu.toString()));
		}
		return email;
	}

	/**
	 * Recupera o e-mail dos usu�rios pertencentes a um grupo
	 * 
	 * @param arrGrupos
	 * @param con
	 * @param grupos
	 * @return
	 * @throws SQLException
	 */

	@SuppressWarnings("unchecked")
	private ArrayList<String> getEmailUsuarioGrupo(JSONArray arrGrupos) throws SQLException {
		try (Connection con = DBUtils.getConnection("workflow")) {
			ArrayList<String> email = new ArrayList<>();

			StringBuilder query = new StringBuilder();
			query.append("select des_email from grupo g ");
			query.append("inner join grupo_usuario gu ");
			query.append("on g.cod_grupo = gu.cod_grupo ");
			query.append("inner join usuario  u ");
			query.append("on u.cod_usuario = gu.cod_usuario ");
			query.append("where g.des_comando = ? ");

			arrGrupos.forEach(nomeGrupo -> {
				try (PreparedStatement pst = con.prepareStatement(query.toString())) {
					pst.setString(1, nomeGrupo.toString());
					try (ResultSet rs = pst.executeQuery()) {
						while (rs.next()) {
							email.add(nulo(rs.getString("des_email"), ""));
						}
					}
				} catch (Exception e) {
					logger.error("Erro ao buscar e-mail dos usuarios: " + e);
				}
			});

			return email;
		}
	}

	/**
	 * Simples parser para converter string em json
	 * 
	 * @param jsonTxt
	 * @return
	 * @throws ParseException
	 */
	public static JSONObject toJSON(String jsonTxt) throws ParseException {
		JSONParser parser = new JSONParser();
		return (JSONObject) parser.parse(jsonTxt);

	}

	public static JSONArray toJSONArr(String jsonTxt) throws ParseException {
		JSONParser parser = new JSONParser();
		JSONArray json = (JSONArray) parser.parse(jsonTxt);

		return json;
	}

	/**
	 * Funcao que retorna a String recebida como parametro caso o objeto seja nulo
	 * 
	 * @param (Object) objeto
	 * @param (String) retorno
	 * @return {@link String}
	 */
	public static String nulo(Object objeto, String retorno) {
		String aux = "";

		if (objeto == null) {
			return retorno;

		} else {

			aux = objeto.toString();

			if (aux.equalsIgnoreCase("null") || aux.equalsIgnoreCase("")) {
				return retorno;

			} else {
				return aux;
			}
		}
	}

}
