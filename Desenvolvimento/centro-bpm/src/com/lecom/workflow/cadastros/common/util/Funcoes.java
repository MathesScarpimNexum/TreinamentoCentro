package com.lecom.workflow.cadastros.common.util;

import java.io.BufferedWriter;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.Normalizer;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import org.apache.log4j.Logger;

import com.lecom.workflow.robo.satelite.WFMail;

import br.com.lecom.workflow.email.EmailMessage;

/**
 * Classe responsavel por agregar os métodos de tratamentos e formatações comuns
 * a todo o projeto
 * 
 * @author TODOS
 * @data Junho 2019
 * @version 1
 */

public class Funcoes {
	private static final Logger logger = Logger.getLogger(Funcoes.class);

	public static void closeConnection(Connection connection, Logger logger) {

		// Encerra a conexao com o Workflow
		try {
			if (connection != null) {
				connection.setAutoCommit(true);
				connection.close();
			}

		} catch (Exception e) {

			logger.error("Erro ao tentar fechar conexão Workflow!", e);

		} finally {

			connection = null;
		}

	}

	public static void closeStatement(Statement statement, ResultSet rs, Logger logger) {

		try {

			if (rs != null) {
				rs.close();
			}

		} catch (SQLException e) {

			logger.info("Erro ao fechar ResultSet", e);

		} finally {

			rs = null;
		}

		try {

			if (statement != null) {
				statement.close();
			}

		} catch (SQLException e1) {

			logger.info("Erro ao fechar Statement", e1);

		} finally {

			statement = null;
		}

	}

	/**
	 * <p>
	 * Criptografa ou Descriptografa uma string.
	 * </p>
	 * Obs: Função legado. não utilizar
	 * 
	 * @deprecated
	 * @author Valter Leite
	 * @param sValor Texto a ser criptografado/descriptografado
	 * @param sAcao  C-Cript, D-decript
	 * @return (String) Texto encriptado
	 */
	public static String criptografia(String sValor, String sAcao) {
		String sCriptografia1 = "";
		String sCriptografia2 = "";

		if (sAcao.equals("C")) {
			sCriptografia1 = "abcdefghijklmnopqrstuvwxyz0123456789ABCDEFGHIJKLMNOPQRSTUWXYZ=_-+[]{}#$%^&*():'. |/";
			sCriptografia2 = "b$c{deo^aPq4rstFvwJxmGMKLuNOQR)6yz01H2p35I78X9Afghi*jklnBCDES(U[WTYZ=_-+]}#%&/.|: '";
		} else {
			sCriptografia1 = "b$c{deo^aPq4rstFvwJxmGMKLuNOQR)6yz01H2p35I78X9Afghi*jklnBCDES(U[WTYZ=_-+]}#%&/.|: '";
			sCriptografia2 = "abcdefghijklmnopqrstuvwxyz0123456789ABCDEFGHIJKLMNOPQRSTUWXYZ=_-+[]{}#$%^&*():'. |/";
		}

		String sRetorno = "";
		int iPosicao = 0;

		if (sValor != null) {

			for (int i = 0; i < sValor.length(); i++) {
				iPosicao = sCriptografia1.indexOf(sValor.substring(i, i + 1));
				if (iPosicao != -1)
					sRetorno = sRetorno + sCriptografia2.substring(iPosicao, iPosicao + 1);
				else
					sRetorno = sRetorno + sValor.substring(i, i + 1);
			}
		}

		return sRetorno;
	}

	public static List<String> emailsGestoresModelo(Integer codForm, Integer codVersao, Connection connection,
			Logger logger) throws Exception {

		PreparedStatement pst = null;
		PreparedStatement pstGest = null;
		ResultSet rs = null;
		ResultSet rsGest = null;
		List<String> retorno = new ArrayList<String>();

		try {

			StringBuilder sqlGest = new StringBuilder();
			sqlGest.append(
					" SELECT '(''' || REPLACE(COD_USU_GESTOR, '/', ''', ''') || ''')' AS GESTORES FROM FORMULARIO ");
			sqlGest.append("  WHERE COD_FORM = ? ");
			sqlGest.append("    AND COD_VERSAO = ? ");

			pstGest = connection.prepareStatement(sqlGest.toString());
			pstGest.setInt(1, codForm);
			pstGest.setInt(2, codVersao);
			rsGest = pstGest.executeQuery();

			if (rsGest.next()) {

				String gestores = rsGest.getString("GESTORES");
				logger.info("gestores: " + gestores);

				StringBuilder sql = new StringBuilder();
				sql.append(" SELECT DES_EMAIL FROM USUARIO ");
				sql.append("  WHERE COD_USUARIO IN " + gestores);

				logger.info("sql: " + sql);
				pst = connection.prepareStatement(sql.toString());
				rs = pst.executeQuery();

				while (rs.next()) {
					retorno.add(rs.getString("DES_EMAIL"));
				}
			}

		} catch (Exception e) {
			e.printStackTrace();

			throw e;

		} finally {
			closeStatement(pstGest, rsGest, logger);
			closeStatement(pst, rs, logger);
		}

		return retorno;
	}

	/**
	 * Executa o Envio de Email
	 * 
	 */
	public static boolean enviaEmail(String msgEmail, String subject, List<String> recipients, boolean html,
			List<String> Cc, List<String> Bcc, String attached, Connection connection, Logger logger) {

		boolean retorno = false;

		String from = getFrom(connection, logger);

		// Mensagem a ser enviada
		String message = msgEmail.toString();
		EmailMessage emailMessage = new EmailMessage(subject, message, from, recipients, html, Cc, Bcc, attached);
		WFMail wfMail = new WFMail();
		String enviaEmail = wfMail.enviaEmailMessage(emailMessage);

		logger.info("enviaEmail : " + enviaEmail);

		if (enviaEmail.indexOf("E-mail colocado na fila de envio.") > -1) {
			retorno = true;
		}

		logger.info("retornoEmail : " + retorno);

		return retorno;
	}

	/**
	 * Executa o Envio de Email
	 * 
	 */
	public static void enviaEmail(String msgEmail, String subject, List<String> recipients, Connection connection,
			Logger logger) {

		String from = getFrom(connection, logger);

		// Mensagem a ser enviada
		String message = msgEmail.toString();
		EmailMessage emailMessage = new EmailMessage(subject, message, from, recipients, true);
		WFMail wfMail = new WFMail();
		String enviaEmail = wfMail.enviaEmailMessage(emailMessage);
		logger.info(enviaEmail);
	}

	/**
	 * Funcao que retorna uma String com espaços a Esquerda
	 * 
	 * @param valor
	 * @param casas
	 * @return {@link String}
	 */
	public static String espacoEsquerda(String valor, int casas) {

		StringBuffer acrescimo = new StringBuffer();
		for (int i = 0; i < casas; i++) {
			acrescimo.append(" ");
		}

		String texto = acrescimo.toString();

		try {

			texto += String.valueOf(valor);
			int tamanho = texto.length();

			if (tamanho - casas >= 0) {
				return texto.substring(tamanho - casas, tamanho);
			} else {
				return texto;
			}

		} catch (Exception e) {
			return texto;
		}
	}

	public final static void executeStatement(Connection connection, final String sqlStatement) throws SQLException {

		PreparedStatement p = null;

		try {
			p = connection.prepareStatement(sqlStatement);
			p.executeUpdate();
		} finally {
			closeStatement(p, null, logger);
		}
	}

	/**
	 * Formata valor recebido para formato 99.99
	 * 
	 * @param valor
	 * @return
	 */
	public static BigDecimal formataDec(double valor) {
		String retorno = "";

		DecimalFormatSymbols dcformat = new DecimalFormatSymbols();
		dcformat.setDecimalSeparator('.');

		NumberFormat formatter = new DecimalFormat("####.00000", dcformat);
		formatter.setMaximumFractionDigits(2);
		retorno = formatter.format(valor);

		BigDecimal retorno2 = new BigDecimal(retorno);
		retorno2.setScale(2);

		return retorno2;
	}

	/**
	 * Funcao que formata a data recebida como dd/MM/yyyy
	 * 
	 * @param Date
	 * @param retorno
	 * @return {@link String}
	 */
	public static String formatarDataBR(Date date) {

		String retorno = "";
		SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy");

		if (date != null) {
			retorno = dateFormat.format(date);
		}

		return retorno;
	}

	/**
	 * Formata valor recebido para formato monetario, separador de milhar(.) e
	 * decimal (,)
	 * 
	 * @param valor
	 * @param param
	 * @return {@link String}
	 */
	public static String formatarDecimalWF(BigDecimal valor, int param) {

		String retorno = "";
		DecimalFormatSymbols dcformat = new DecimalFormatSymbols();
		dcformat.setDecimalSeparator('.');

		NumberFormat formatter = new DecimalFormat("#######0.00000", dcformat);
		formatter.setMaximumFractionDigits(param);

		if (valor != null) {
			retorno = formatter.format(valor);
		}

		return retorno;
	}

	/**
	 * Formata valor recebido para formato monetario, separador de milhar(.) e
	 * decimal (,)
	 * 
	 * @param valor
	 * @param param
	 * @return {@link String}
	 */
	public static String formatarMoedaBR(BigDecimal valor, int param) {

		String retorno = "";
		DecimalFormatSymbols dcformat = new DecimalFormatSymbols();
		dcformat.setDecimalSeparator(',');
		dcformat.setGroupingSeparator('.');

		NumberFormat formatter = new DecimalFormat("#,###.00000", dcformat);
		formatter.setMaximumFractionDigits(param);

		if (valor != null) {
			retorno = formatter.format(valor);

		}

		return retorno;
	}

	/**
	 * Metodo que busca os dados do Fluxo de Parâmetro
	 * 
	 * @param String     nomeTabelaParam
	 * @param String     nomeCampo
	 * @param Connection connection
	 * @param Logger     logger
	 * @return
	 * @throws Exception
	 */
	public static String getDadosFluxoParametro(String nomeCampo, Connection connection, Logger logger,
			Integer codFormParam) throws Exception {

		PreparedStatement pst = null;
		PreparedStatement pstTab = null;
		ResultSet rs = null;
		ResultSet rsTab = null;
		String ret = null;

		try {
			StringBuilder sqlTab = new StringBuilder();
			sqlTab.append(" SELECT DES_NOME_TABELA FROM FORMULARIO ");
			sqlTab.append("  WHERE COD_FORM = ? ");

			pstTab = connection.prepareStatement(sqlTab.toString());
			pstTab.setInt(1, codFormParam);
			rsTab = pstTab.executeQuery();
			rsTab.next();

			String nomeTabelaParam = rsTab.getString("DES_NOME_TABELA");
			// logger.info("nomeTabelaParam: " + nomeTabelaParam);

			StringBuilder sql = new StringBuilder();
			sql.append(" SELECT ");
			sql.append(nomeCampo);
			sql.append("   FROM ");
			sql.append(nomeTabelaParam);
			sql.append("  WHERE COD_PROCESSO_F = ( ");
			sql.append("		   SELECT MAX(COD_PROCESSO) FROM PROCESSO ");
			sql.append("			WHERE COD_FORM = ");
			sql.append(codFormParam);
			sql.append("			  AND IDE_FINALIZADO = 'P') ");
			sql.append("	AND COD_ETAPA_F = 2 ");

			// logger.info("sql: " + sql);

			pst = connection.prepareStatement(sql.toString());
			rs = pst.executeQuery();
			rs.next();

			ret = rs.getString(nomeCampo);

		} catch (Exception e) {
			e.printStackTrace();
			logger.info("[Erro] : ", e);

			throw e;

		} finally {
			closeStatement(pstTab, rsTab, logger);
			closeStatement(pst, rs, logger);
		}

		return ret;
	}

	/**
	 * Metodo que le tabela de parametros do workflow e retorna um mapa com as
	 * informacoes
	 * 
	 * @param connection
	 * @return
	 * @throws Exception
	 */
	public static Map<String, String> getDadosUsuario(Connection connection, Integer codUsuario) throws Exception {

		Map<String, String> parametroMap = new HashMap<String, String>();

		PreparedStatement pst = null;
		ResultSet rs = null;

		try {
			StringBuilder sql = new StringBuilder();
			sql.append(" SELECT * ");
			sql.append("   FROM USUARIO ");
			sql.append("  WHERE COD_USUARIO = ? ");

			pst = connection.prepareStatement(sql.toString());
			pst.setInt(1, codUsuario);

			rs = pst.executeQuery();

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

		} catch (Exception e) {
			throw e;

		} finally {

			Funcoes.closeStatement(pst, rs, logger);
		}
		return parametroMap;
	}


	/**
	 * Metodo que le tabela de usuarios do workflow e retorna um mapa com as informacoes, com as chaves do Map sendo o nome das colunas da tabela
	 * @param connection
	 * @param loginUsuario
	 * @return
	 * @throws Exception
	 */

	public static Map<String, String> getDadosUsuario(Connection connection, String loginUsuario) throws Exception {

		Map<String, String> parametroMap = new HashMap<String, String>();

		StringBuilder sql = new StringBuilder();
		sql.append(" SELECT * ");
		sql.append("   FROM USUARIO ");
		sql.append("  WHERE DES_LOGIN = ? ");

		try (PreparedStatement pst = connection.prepareStatement(sql.toString());) {
			pst.setString(1, loginUsuario);

			try (ResultSet rs = pst.executeQuery();) {

				ResultSetMetaData metaData = rs.getMetaData();
				int columnCount = metaData.getColumnCount();

				if(rs.next()){
					int columnIndex = 1;

					while(columnIndex < (columnCount + 1)) {
						parametroMap.put(metaData.getColumnName(columnIndex).toUpperCase(), nulo(rs.getString(columnIndex), ""));
						columnIndex++;
					}
				}
			}
			
		} catch (Exception e) {
			throw e;
		}

		return parametroMap;
	}


	/**
	 * Método que resgata os Dados do Usuario - Via CódUsuário WF
	 * 
	 * @return ret[0]=> Código do Usuario <br>
	 *         ret[1]=> Nome do Usuario <br>
	 *         ret[2]=> Login do Usuario <br>
	 *         ret[3]=> Email do Usuario <br>
	 *         ret[4]=> Código Líder <br>
	 *         ret[5]=> Código Usuário Perfil <br>
	 *         ret[6]=> DES_CAMPO_INTEGRACAO1 = AN8 JDE <br>
	 *         ret[7]=> DES_CAMPO_INTEGRACAO2 = Matricula Senior <br>
	 *         ret[8]=> DES_CAMPO_INTEGRACAO3 = ? <br>
	 *         ret[9]=> Código do depto <br>
	 *         ret[10]=> Nome do depto <br>
	 */
	public static String[] getDadosUsuario(Integer codUsuarioWF, String desLoginWF, Integer codUsuarioLiderWF,
			Integer codUsuarioPerfilWF, String desCampIntegra1, String desCampIntegra2, String desCampIntegra3,
			Connection connection, Logger logger) {

		String[] ret = new String[11];
		PreparedStatement pst = null;
		ResultSet rs = null;
		logger.info("Connection: " + connection);

		try {
			StringBuilder sql = new StringBuilder();
			sql.append(" SELECT U.COD_USUARIO, U.NOM_USUARIO, U.DES_LOGIN, U.DES_EMAIL, U.COD_LIDER, ");
			sql.append(
					" 		U.CODIGO_USUARIO_PERFIL, U.DES_CAMPO_INTEGRACAO1, U.DES_CAMPO_INTEGRACAO2, U.DES_CAMPO_INTEGRACAO3, ");
			sql.append(" 		D.COD_DEPTO, D.DES_DEPTO ");
			sql.append("   FROM USUARIO U ");
			sql.append("   JOIN DEPTO D ON (D.COD_DEPTO = U.COD_DEPTO) ");
			sql.append("  WHERE U.COD_USUARIO IS NOT NULL ");

			if (codUsuarioWF != null) {
				sql.append("  AND U.COD_USUARIO = ");
				sql.append(codUsuarioWF);
			}

			if (desLoginWF != null) {
				sql.append("  AND UPPER(U.DES_LOGIN) LIKE '");
				sql.append(desLoginWF.toUpperCase());
				sql.append("'");
			}

			if (codUsuarioLiderWF != null) {
				sql.append("  AND U.COD_LIDER = ");
				sql.append(codUsuarioLiderWF);
			}

			if (codUsuarioPerfilWF != null) {
				sql.append("  AND U.CODIGO_USUARIO_PERFIL = ");
				sql.append(codUsuarioPerfilWF);
			}

			if (desCampIntegra1 != null) {
				sql.append("  AND UPPER(U.DES_CAMPO_INTEGRACAO1) LIKE '");
				sql.append(desCampIntegra1.toUpperCase());
				sql.append("'");
			}

			if (desCampIntegra2 != null) {
				sql.append("  AND UPPER(U.DES_CAMPO_INTEGRACAO2) LIKE '");
				sql.append(desCampIntegra2.toUpperCase());
				sql.append("'");
			}

			if (desCampIntegra3 != null) {
				sql.append("  AND UPPER(U.DES_CAMPO_INTEGRACAO3) LIKE '");
				sql.append(desCampIntegra3.toUpperCase());
				sql.append("'");
			}

			logger.info("usuarioDados[10] SQL : " + sql);

			pst = connection.prepareStatement(sql.toString());
			rs = pst.executeQuery();

			if (rs.next()) {
				ret[0] = Funcoes.nulo(rs.getString("COD_USUARIO"), "");
				ret[1] = Funcoes.nulo(rs.getString("NOM_USUARIO"), "");
				ret[2] = Funcoes.nulo(rs.getString("DES_LOGIN"), "");
				ret[3] = Funcoes.nulo(rs.getString("DES_EMAIL"), "");
				ret[4] = Funcoes.nulo(rs.getString("COD_LIDER"), "");
				ret[5] = Funcoes.nulo(rs.getString("CODIGO_USUARIO_PERFIL"), "");
				ret[6] = Funcoes.nulo(rs.getString("DES_CAMPO_INTEGRACAO1"), "");
				ret[7] = Funcoes.nulo(rs.getString("DES_CAMPO_INTEGRACAO2"), "");
				ret[8] = Funcoes.nulo(rs.getString("DES_CAMPO_INTEGRACAO3"), "");
				ret[9] = Funcoes.nulo(rs.getString("COD_DEPTO"), "");
				ret[10] = Funcoes.nulo(rs.getString("DES_DEPTO"), "");
			}

		} catch (Exception e) {
			e.printStackTrace();
			logger.error("[Erro ao buscar dados do Usuário]: ", e);

		} finally {

			Funcoes.closeStatement(pst, rs, logger);
		}

		return ret;
	}

	public static String getDataAtual() {

		Date data = new Date(System.currentTimeMillis());
		SimpleDateFormat formatarDate = new SimpleDateFormat("dd/MM/yyyy");

		return formatarDate.format(data);
	}

	/*
	 * * * * * * * Função calcula diferença entre 2 horários, no formato HH:MM, em
	 * caso de tempo negativo retorno vazio Ex : Hora Inicio = 08:00 Hora Fim =
	 * 09:00 Retorno = 01:00 * * * * *
	 */
	public static String getDiffEntre2Horarios(String horaInicio, String horaFim) {

		String retorno = "";

		if (horaInicio.length() != 5 || horaFim.length() != 5)
			return "00:00";

		Integer segundosInicial = new Integer(horaInicio.substring(0, 2)) * 3600;
		segundosInicial += new Integer(horaInicio.substring(3, 5)) * 60;

		Integer segundosFinal = new Integer(horaFim.substring(0, 2)) * 3600;
		segundosFinal += new Integer(horaFim.substring(3, 5)) * 60;

		Integer horasTotal = (segundosFinal - segundosInicial) / 3600;
		Integer minutosTotal = ((segundosFinal - segundosInicial) % 3600) / 60;

		if (horasTotal >= 0) {

			String auxHorasTotal = (horasTotal < 10) ? "0" + horasTotal : "" + horasTotal;
			String auxMinutosTotal = (minutosTotal < 10) ? "0" + minutosTotal : "" + minutosTotal;

			retorno = auxHorasTotal + ":" + auxMinutosTotal;
		}

		return retorno;
	}

	/**
	 * Recupera o e-mail informado no cadastro de parâmetros
	 * 
	 * @return DES_FROM
	 */
	public static String getFrom(Connection connection, Logger logger) {

		String sSQL = null;
		String sRetorno = "";
		ResultSet rsURL = null;
		PreparedStatement pstURL = null;

		try {

			sSQL = "SELECT DES_FROM FROM PARAMETRO";
			pstURL = connection.prepareStatement(sSQL, ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY);
			rsURL = pstURL.executeQuery();

			// Seta a url do WF do cliente
			if (rsURL.next()) {
				sRetorno = rsURL.getString("DES_FROM").trim().toString();
			}

		} catch (SQLException e) {
			logger.error("[Robo Envia E-mail ] Falha ao tentar recurapar o DES_FROM.", e);
			e.printStackTrace();

		} finally {
			closeStatement(pstURL, rsURL, logger);
		}
		return sRetorno;
	}

	public static String getHoraAtual(String tipo) {

		Calendar Cal = Calendar.getInstance();

		// horas, minutos e segundos
		String retorno = "";

		int x = 0;
		if (tipo.equals("H")) {
			x = Cal.get(Calendar.HOUR_OF_DAY);
		} else if (tipo.equals("M")) {
			x = Cal.get(Calendar.MINUTE);
		} else if (tipo.equals("S")) {
			x = Cal.get(Calendar.SECOND);
		}

		retorno = (x < 10) ? "0" + x : String.valueOf(x);

		return retorno;
	}

	/**
	 * Pega E-Mail definido para ser usado no modo de testes, para cada formulário e
	 * versão necessários
	 * 
	 * @param Connection cnWF
	 * @param Integer    codForm
	 * @param Integer    codVersao
	 * @return String EMAIL_TESTE
	 */
	public static String getMailModoTestes(Connection cnWF, Integer codForm, Integer codVersao) {

		String retorno = "";

		ResultSet rs = null;
		PreparedStatement pst = null;

		try {

			String sSQL = " SELECT EMAIL_TESTE FROM FORMULARIO WHERE COD_FORM = ? AND COD_VERSAO = ? ";

			pst = cnWF.prepareStatement(sSQL, ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY);
			pst.setInt(1, codForm);
			pst.setInt(2, codVersao);

			rs = pst.executeQuery();
			if (rs.next()) {
				retorno = rs.getString("EMAIL_TESTE").toString();
			}

		} catch (Exception e) {

			e.printStackTrace();
			logger.error("[ERRO] : ", e);

		} finally {

			Funcoes.closeStatement(pst, rs, logger);
		}

		return retorno;
	}

	/**
	 * Metodo que lê arquivo properties com parametros e retorna um mapa.
	 * 
	 * @param nomeArquivo
	 * @return
	 * @throws Exception
	 */
	public static Map<String, String> getParametrosIntegracao(String nomeArquivoParam) throws Exception {
		if (!nomeArquivoParam.endsWith(".properties")) {
			nomeArquivoParam += ".properties";
		}

		FileInputStream arquivo = new FileInputStream(nomeArquivoParam);
		Properties properties = new Properties();
		properties.load(arquivo);

		Set<Object> keySet = properties.keySet();
		Iterator<Object> iterator = keySet.iterator();
		Map<String, String> parametroMap = new HashMap<String, String>();

		while (iterator.hasNext()) {
			String key = (String) iterator.next();
			parametroMap.put(key, properties.getProperty(key));
		}
		return parametroMap;
	}

	/**
	 * Metodo que le tabela de parametros do workflow e retorna um mapa com as
	 * informacoes
	 * 
	 * @param connection
	 * @return
	 * @throws Exception
	 */
	public static Map<String, String> getParametrosWF(Connection connection) throws Exception {

		Map<String, String> parametroMap = new HashMap<String, String>();
		PreparedStatement pst = null;
		ResultSet rs = null;

		try {

			String sql = " SELECT * FROM PARAMETRO ";

			pst = connection.prepareStatement(sql);
			rs = pst.executeQuery();

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

		} catch (Exception e) {
			logger.info("[Erro] : ", e);
			throw e;

		} finally {
			closeStatement(pst, rs, logger);
		}

		return parametroMap;
	}

	/**
	 * Busca o status do processo/etapa
	 * 
	 * @param cnwf
	 * @param codProcesso
	 * @param codEtapa
	 * @param codCiclo
	 * @return
	 * @throws Exception
	 */
	public static String getStatusProcesso(Connection cnwf, int codProcesso, int codEtapa, int codCiclo)
			throws Exception {
		String retorno = "";

		PreparedStatement pst = null;
		ResultSet rs = null;

		try {
			StringBuilder sql = new StringBuilder();
			sql.append(" SELECT ide_status ");
			sql.append("   FROM processo_etapa ");
			sql.append("  WHERE cod_processo = ? ");
			sql.append("	AND cod_etapa = ? ");
			sql.append("	AND cod_ciclo = ? ");

			pst = cnwf.prepareStatement(sql.toString());
			pst.setInt(1, codProcesso);
			pst.setInt(2, codEtapa);
			pst.setInt(3, codCiclo);

			rs = pst.executeQuery();

			if (rs.next())
				retorno = rs.getString("ide_status");

		} catch (Exception e) {
			throw e;

		} finally {

			Funcoes.closeStatement(pst, rs, logger);
		}
		return retorno;
	}

	public static Integer getVersaoForm(Integer codProcesso, Connection connection, Logger logger) {
		Integer ret = 0;
		PreparedStatement pst = null;
		ResultSet rs = null;

		StringBuilder sql = new StringBuilder();
		sql.append(" SELECT COD_VERSAO FROM PROCESSO ");
		sql.append("  WHERE COD_PROCESSO = ? ");

		try {
			pst = connection.prepareStatement(sql.toString());
			pst.setInt(1, codProcesso);
			rs = pst.executeQuery();
			rs.next();
			ret = rs.getInt("COD_VERSAO");

		} catch (SQLException e) {
			e.printStackTrace();
			logger.info("[Erro] : ", e);

		} finally {

			Funcoes.closeStatement(pst, rs, logger);
		}

		return ret;
	}

	/**
	 * Função que retorna o diretório base do Workflow Pode ser utilizado para a
	 * criação de arquivos (para impressão, relatórios, etc.)
	 * 
	 * @return diretório base do Workflow (no tomcat, [tomcat]/webapps/workflow/)
	 */
	public static String getWFRootDir() {
		String retorno = Funcoes.class.getClassLoader().getResource("").getPath(); // classes
		retorno += "../"; // WEB-INF
		retorno += "../"; // workflow

		return retorno;
	}

	public static boolean gravaHistorico(Connection connection, Logger logger, Integer codProcesso, Integer codEtapa,
			Integer codCiclo, String obs, String usuarioExecutor) {

		boolean retorno = false;

		try {

			StringBuilder sqlEtapaCiclo = new StringBuilder();
			sqlEtapaCiclo.append(" SELECT COUNT(*) N_HIST ");
			sqlEtapaCiclo.append("	 FROM LEC_HIST_ETAPA_CICLO ");
			sqlEtapaCiclo.append("	WHERE COD_PROCESSO = ? ");
			sqlEtapaCiclo.append("	  AND COD_ETAPA = ? ");
			sqlEtapaCiclo.append("	  AND COD_CICLO = ? ");

			try (PreparedStatement pstSelEtapaCiclo = connection.prepareStatement(sqlEtapaCiclo.toString())) {

				pstSelEtapaCiclo.setInt(1, codProcesso);
				pstSelEtapaCiclo.setInt(2, codEtapa);
				pstSelEtapaCiclo.setInt(3, codCiclo);

				try (ResultSet rsSelEtapaCiclo = pstSelEtapaCiclo.executeQuery()) {

					if (rsSelEtapaCiclo.next()) {

						if (rsSelEtapaCiclo.getInt("N_HIST") == 0) {

							retorno = gravarLecHistorico(connection, logger, codProcesso, codEtapa, codCiclo, obs,
									usuarioExecutor, 0);
						}
					}
				}
			}

		} catch (Exception e) {
			e.printStackTrace();
			logger.error("[ERRO] : ", e);

			retorno = false;
		}

		return retorno;
	}

	/*
	 * blocoParalelo, caso "0" serve para 2 possibilidades ( 1 - não há separação de
	 * grupos paralelos, 2 - Guarda os dados das etapas que precedem o paralelo e da
	 * concentradora para frente )
	 */
	public static boolean gravaHistoricoParalelo(Connection connection, Logger logger, Integer codProcesso,
			Integer codEtapa, Integer codCiclo, String obs, String usuarioExecutor, Integer blocoParalelo) {

		try {
			return gravarLecHistorico(connection, logger, codProcesso, codEtapa, codCiclo, obs, usuarioExecutor,
					blocoParalelo);

		} catch (SQLException e) {
			e.printStackTrace();
			logger.error("[ERRO] : ", e);

			return false;
		}
	}

	/*
	 * blocoParalelo, caso "0" serve para 2 possibilidades ( 1 - não há separação de
	 * grupos paralelos, 2 - Guarda os dados das etapas que precedem o paralelo e da
	 * concentradora para frente )
	 */
	private static boolean gravarLecHistorico(Connection connection, Logger logger, Integer codProcesso, Integer codEtapa, Integer codCiclo, String obs, String usuarioExecutor, Integer blocoParalelo) throws SQLException {

		boolean retorno = false;
		String histAtual = "";
		StringBuilder histGravarDB = new StringBuilder();
		StringBuilder insertUpdateHist = new StringBuilder();

		try {

			StringBuilder sqlHistAtual = new StringBuilder();
			sqlHistAtual.append(" SELECT COD_PROCESSO, HISTORICO_TEXTO ");
			sqlHistAtual.append("   FROM LEC_HISTORICO ");
			sqlHistAtual.append("  WHERE COD_PROCESSO = ? ");
			sqlHistAtual.append("    AND BLOCO_PARALELO = ? ");

			try (PreparedStatement pstHistAtual = connection.prepareStatement(sqlHistAtual.toString())) {

				// pstHistAtual = connection.prepareStatement(sqlHistAtual.toString());
				pstHistAtual.setInt(1, codProcesso);
				pstHistAtual.setInt(2, blocoParalelo);
				// rsHistAtual = pstHistAtual.executeQuery();

				try (ResultSet rsHistAtual = pstHistAtual.executeQuery()) {

					if (rsHistAtual.next()) {

						histAtual = rsHistAtual.getString("HISTORICO_TEXTO");

						histGravarDB.append("<font color=Firebrick >");
						histGravarDB.append(usuarioExecutor);
						histGravarDB.append("</font ><br>");
						histGravarDB.append(obs);
						histGravarDB.append("<br><br>");
						histGravarDB.append(histAtual);

						insertUpdateHist.append(" UPDATE LEC_HISTORICO ");
						insertUpdateHist.append("	 SET HISTORICO_TEXTO = ? ");
						insertUpdateHist.append("  WHERE COD_PROCESSO = ? ");
						insertUpdateHist.append("    AND BLOCO_PARALELO = ? ");

					} else {

						insertUpdateHist.append(" INSERT INTO LEC_HISTORICO(HISTORICO_TEXTO, COD_PROCESSO, BLOCO_PARALELO) ");
						insertUpdateHist.append(" VALUES (?, ?, ?) ");

						histGravarDB.append("<font color=Firebrick >");
						histGravarDB.append(usuarioExecutor);
						histGravarDB.append("</font ><br>");
						histGravarDB.append(obs);
					}
					// Funcoes.closeStatement(pstHistAtual, rsHistAtual, logger);
				}
			}

			try (PreparedStatement pstInsertUpdateHist = connection.prepareStatement(insertUpdateHist.toString())) {

				// pstInsertUpdateHist = connection.prepareStatement(insertUpdateHist.toString());
				pstInsertUpdateHist.setString(1, histGravarDB.toString());
				pstInsertUpdateHist.setInt(2, codProcesso);
				pstInsertUpdateHist.setInt(3, blocoParalelo);
				pstInsertUpdateHist.executeUpdate();

				// Funcoes.closeStatement(pstInsertUpdateHist, null, logger);
			}

			// Grava a Etapa e Ciclo que foi atualizado o historico, como validação para que
			// não seja duplicada a Observação no processo.
			StringBuilder insEtapaCiclo = new StringBuilder();
			insEtapaCiclo.append(" INSERT INTO LEC_HIST_ETAPA_CICLO(COD_PROCESSO, COD_ETAPA, COD_CICLO) ");
			insEtapaCiclo.append(" VALUES (?,?,?) ");

			try (PreparedStatement pstInsEtapaCiclo = connection.prepareStatement(insertUpdateHist.toString())) {

				// pstInsEtapaCiclo = connection.prepareStatement(insEtapaCiclo.toString());
				pstInsEtapaCiclo.setInt(1, codProcesso);
				pstInsEtapaCiclo.setInt(2, codEtapa);
				pstInsEtapaCiclo.setInt(3, codCiclo);
				pstInsEtapaCiclo.executeUpdate();

				// Funcoes.closeStatement(pstInsEtapaCiclo, null, logger);
			}

			retorno = true;

		} catch (Exception e) {
			throw e;
		}

		return retorno;
	}

	/**
	 * Funcao que retorna uma string preenchida com a string especificada à
	 * esquerda, com tamanho de acordo com o parâmetro informado.
	 * 
	 * @param source
	 * @param filler
	 * @param casas
	 * @return {@link String}
	 */
	public static String leftFill(String source, String filler, int casas) {

		StringBuilder result = new StringBuilder();
		for (int i = 0; i < casas; i++) {
			result.append(filler);
		}

		String texto = result.toString();

		try {

			texto += source;
			int tamanho = texto.length();

			if (tamanho - casas >= 0) {
				return texto.substring(tamanho - casas, tamanho);
			} else {
				return texto;
			}

		} catch (Exception e) {
			return texto;
		}
	}

	/**
	 * Transforma valor recebido em formato moeda BR para BigDecimal
	 * 
	 * @param valor
	 */
	public static BigDecimal moedaBRtoBigDecimal(String valor) {

		BigDecimal retorno = new BigDecimal(0);

		if (valor != null && !valor.equals("")) {
			valor = valor.replaceAll("[,]", ".");

			int lastIndex = valor.lastIndexOf(".");

			while (valor.indexOf(".") != -1 && valor.indexOf(".") != lastIndex) {
				int index = valor.indexOf(".");
				valor = valor.substring(0, index) + valor.substring(index + 1);
				lastIndex = valor.lastIndexOf(".");
			}

			retorno = new BigDecimal(valor);
		}

		return retorno;
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

	/**
	 * Funcao que formata a data recebida como dd/MM/yyyy
	 * 
	 * @param Date
	 * @param retorno
	 * @return {@link String}
	 */
	public static Date parseDataBR(String data) {

		Date retorno = null;
		SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy");

		try {
			retorno = dateFormat.parse(data);
		} catch (Exception e) {
		}

		return retorno;
	}

	public static String readTemplate(String resourceName) throws IOException {

		InputStream is = Funcoes.class.getClass().getResourceAsStream(resourceName);
		StringBuffer sb = new StringBuffer();

		while (is.available() > 0) {
			byte[] b = new byte[is.available()];
			is.read(b);
			sb.append(new String(b));
		}

		return sb.toString();
	}

	/**
	 * Funcao que remove acentos de um texto
	 * 
	 * @param texto
	 * @return {@link String}
	 */
	public static String removeAcentos(String texto) {

		if (!texto.equals("")) {
			String output = texto;
			output = Normalizer.normalize(output, Normalizer.Form.NFD);
			output = output.replaceAll("[^\\p{ASCII}]", "");

			return output;

		} else {
			return texto;
		}
	}

	public static final void rollback(final Connection connection, final Logger logger) {

		try {
			connection.rollback();
		} catch (SQLException e1) {
			logger.warn("Erro ao efetuar rollback.", e1);
		}
	}

	/**
	 * Retorna "selected" caso as informações forem iguais
	 * 
	 * @author Carlos Henrique Leda
	 * @param valor1 1º valor a ser comparado
	 * @param valor2 2º valor a ser comparado
	 * @return (String) String decorrente da comparação
	 */
	public static String selecionado(String valor1, String valor2) {

		try {

			if (valor1.trim().equals(valor2.trim())) {
				return "selected='selected'";
			} else {
				return "";
			}

		} catch (Exception e) {
			return "";
		}
	}

	/*
	 * * * * * * * Função soma 2 quantidades de horas, no formato HH:MM Ex : hrA =
	 * 01:00 hrB = 02:00 Retorno = 03:00 * * * * *
	 */
	public static String somaHoras(String hrA, String hrB, boolean zerarHora) {

		if (hrA.length() != 5 || hrB.length() != 5)
			return "00:00";

		Integer temp = 0;
		Integer auxNovaHora = 0;
		String novaHora = "00";
		String novoMinuto = "00";

		Integer hora1 = new Integer(hrA.substring(0, 2)) * 1;
		Integer hora2 = new Integer(hrB.substring(0, 2)) * 1;
		Integer minu1 = new Integer(hrA.substring(3, 5)) * 1;
		Integer minu2 = new Integer(hrB.substring(3, 5)) * 1;

		temp = minu1 + minu2;
		while (temp > 59) {
			auxNovaHora++;
			temp = temp - 60;
		}
		novoMinuto = temp.toString().length() == 2 ? "" + temp : "0" + temp;

		temp = hora1 + hora2 + auxNovaHora;
		while (temp > 23 && zerarHora) {
			temp = temp - 24;
		}
		novaHora = temp.toString().length() == 2 ? "" + temp : "0" + temp;

		return novaHora + ":" + novoMinuto;
	}

	/**
	 * Método que resgata os Dados do Iniciador do Processo
	 * 
	 * @return ret[0]=> Nome do Iniciador <br>
	 *         ret[1]=> Login do Iniciador <br>
	 *         ret[2]=> Email do Iniciador <br>
	 *         ret[3]=> Data de Inicio
	 */
	public static String[] usuarioLoginIniciador(Integer codProcesso, Connection connection, Logger logger) {

		String[] ret = new String[4];
		PreparedStatement pst = null;
		ResultSet rs = null;

		try {
			StringBuilder sql = new StringBuilder();
			sql.append(
					"     SELECT U.NOM_USUARIO, U.DES_LOGIN, U.DES_EMAIL, TO_CHAR(P.DAT_DATA, 'DD/MM/YYYY HH24:mm') as DAT_DATA ");
			sql.append("   	   FROM USUARIO U ");
			sql.append(" INNER JOIN PROCESSO P ON P.COD_USUARIO = U.COD_USUARIO ");
			sql.append("	  WHERE P.COD_PROCESSO = ? ");

			pst = connection.prepareStatement(sql.toString());
			pst.setInt(1, codProcesso);
			rs = pst.executeQuery();

			if (rs.next()) {
				ret[0] = Funcoes.nulo(rs.getString("NOM_USUARIO"), "");
				ret[1] = Funcoes.nulo(rs.getString("DES_LOGIN"), "");
				ret[2] = Funcoes.nulo(rs.getString("DES_EMAIL"), "");
				ret[3] = Funcoes.nulo(rs.getString("DAT_DATA"), "");
			}

		} catch (Exception e) {

			e.printStackTrace();
			logger.error("[Erro ao buscar Login Iniciador]: ", e);

		} finally {

			Funcoes.closeStatement(pst, rs, logger);
		}

		return ret;
	}

	public static String verificaDiaSemana(Integer vlrDia) {

		switch (vlrDia) {
		case 2:
			return "Segunda-feira";
		case 3:
			return "Terça-feira";
		case 4:
			return "Quarta-feira";
		case 5:
			return "Quinta-feira";
		case 6:
			return "Sexta-feira";
		default:
			return "Todos os Dias";
		}
	}

	/**
	 * Cria arquivo contendo texto passado como parâmetro.
	 * 
	 * @param filename
	 * @param text
	 * @param append
	 * @return
	 */
	public static boolean writeToFile(String filename, String text, boolean append) {

		FileWriter fileWriter = null;
		BufferedWriter bufferedWriter = null;

		try {
			fileWriter = new FileWriter(filename, append);
			bufferedWriter = new BufferedWriter(fileWriter);
			bufferedWriter.write(text);

			return true;

		} catch (Exception e) {
			logger.info("[Erro] : ", e);
			return false;

		} finally {

			try {
				bufferedWriter.flush();
				bufferedWriter.close();

			} catch (Exception e) {
				logger.info("[Erro] : ", e);
			}
		}
	}

	/**
	 * Funcao que retorna um numero inteiro passado como parametro como uma string
	 * no formato "0#" com zeros à esquerda, com tamanho de acordo com o parâmetro
	 * informado.
	 * 
	 * @param numero
	 * @param casas
	 * @return {@link String}
	 */
	public static String zeroFill(int numero, int casas) {

		StringBuffer zeros = new StringBuffer();
		for (int i = 0; i < casas; i++) {
			zeros.append("0");
		}

		String texto = zeros.toString();

		try {

			texto += String.valueOf(numero);
			int tamanho = texto.length();

			if (tamanho - casas >= 0) {
				return texto.substring(tamanho - casas, tamanho);
			} else {
				return texto;
			}

		} catch (Exception e) {
			return texto;
		}
	}

	public static String zerosEsquerda(String valor, int casas) {

		StringBuffer acrescimo = new StringBuffer();
		for (int i = 0; i < casas; i++) {
			acrescimo.append("0");
		}

		String texto = acrescimo.toString();

		try {

			texto += String.valueOf(valor);
			int tamanho = texto.length();

			if (tamanho - casas >= 0) {
				return texto.substring(tamanho - casas, tamanho);
			} else {
				return texto;
			}

		} catch (Exception e) {
			return texto;
		}
	}

	/**
	 * Verifica se o processo execurado está no modo de testes ou não
	 * 
	 * @param Connection cnWF
	 * @param Integer    codProcesso
	 * @return boolean true / false ( onde T = Modo Testes, F = Modo Normal )
	 */
	public static boolean isModoTeste(Logger logger, Connection cnWF, String codProcesso) {

		boolean retorno = false;

		try {

			// VERIFICA SE O PROCESSO ESTA EM MODO TESTE
			String isTeste = "S";
			String sSQL = "SELECT IDE_BETA_TESTE FROM PROCESSO WHERE COD_PROCESSO = " + codProcesso;

			try (PreparedStatement pst = cnWF.prepareStatement(sSQL, ResultSet.TYPE_SCROLL_INSENSITIVE,
					ResultSet.CONCUR_READ_ONLY);) {

				try (ResultSet rs = pst.executeQuery();) {

					if (rs.next()) {
						isTeste = rs.getString("IDE_BETA_TESTE").toString();
					}

					// CASO FOR N O MODELO NAO ESTA EM MODO TESTE
					if ("N".equalsIgnoreCase(isTeste)) {
						retorno = false;
					} else {
						retorno = true;
					}
				}
			}

		} catch (Exception e) {

			e.printStackTrace();
			logger.error("[ERRO] : ", e);
		}

		return retorno;
	}

	/**
	 * Verifica se a aplicação externa executada está no modo de testes ou não
	 * 
	 * @param Connection cnWF
	 * @param String     url ( parte final da URL da aplicação Externa, pois o
	 *                   começo será sempre igual, com o endereço da aplicação )
	 * @return String S / N ( onde S = Sim, N = Não ( default ) )
	 */
	public static String isModoTesteApp(Connection cnWF, String url) {

		String retorno = "N";

		try {

			// VERIFICA SE A APLICAÇÃO EXTERNA ESTÁ NO MODO DE TESTES
			StringBuilder sSQL = new StringBuilder();
			sSQL.append(" SELECT IDE_BETA_TESTE ");
			sSQL.append("	FROM APLICACAO_EXTERNA ");
			sSQL.append("  WHERE URL LIKE '%");
			sSQL.append(url);
			sSQL.append("'");

			try (PreparedStatement pst = cnWF.prepareStatement(sSQL.toString());) {

				try (ResultSet rs = pst.executeQuery();) {

					if (rs.next()) {
						retorno = rs.getString("IDE_BETA_TESTE").toString();
					}
				}
			}
			// logger.info("retorno : " + retorno);

		} catch (Exception e) {

			e.printStackTrace();
			// logger.error("[ERRO] : ", e);

		}

		return retorno;
	}

	/**
	 * Pega E-Mail definido para ser usado no modo de testes, para cada formulário e
	 * versão necessários
	 * 
	 * @param Connection cnWF
	 * @param Integer    codForm
	 * @param Integer    codVersao
	 * @return String EMAIL_TESTE
	 */
	public static String getMailModoTestes(Logger logger, Connection cnWF, String codForm, String codVersao) {

		String retorno = "";

		try {

			String sSQL = " SELECT EMAIL_TESTE FROM FORMULARIO WHERE COD_FORM = ? AND COD_VERSAO = ? ";

			try (PreparedStatement pst = cnWF.prepareStatement(sSQL, ResultSet.TYPE_SCROLL_INSENSITIVE,
					ResultSet.CONCUR_READ_ONLY);) {
				pst.setString(1, codForm);
				pst.setString(2, codVersao);

				try (ResultSet rs = pst.executeQuery();) {

					if (rs.next()) {

						retorno = rs.getString("EMAIL_TESTE").toString();
					}
				}
			}

		} catch (Exception e) {

			e.printStackTrace();
			logger.error("[ERRO] : ", e);
		}

		return retorno;
	}

	public static String getVersaoForm(Logger logger, Connection cnWF, String codProcesso) {

		String retorno = "";

		try {

			StringBuilder sSQL = new StringBuilder();
			sSQL.append(" SELECT COD_VERSAO FROM PROCESSO ");
			sSQL.append("  WHERE COD_PROCESSO = ? ");

			try (PreparedStatement pst = cnWF.prepareStatement(sSQL.toString());) {
				pst.setString(1, codProcesso);

				try (ResultSet rs = pst.executeQuery();) {

					if (rs.next()) {
						retorno = rs.getString("COD_VERSAO");
					}
				}
			}

		} catch (Exception e) {
			e.printStackTrace();
			logger.info("[Erro] : ", e);
		}

		return retorno;
	}

	/**
	 * Pega a exceção gerada e retorna em formato String, para ser colocada no log.
	 * 
	 * @param e
	 * @return
	 */
	public static String exceptionPrinter(Exception e) {
		StringBuilder sb = new StringBuilder();
		sb.append(e.toString());
		StackTraceElement[] el = e.getStackTrace();
		for (int i = 0; i < el.length; i++) {
			sb.append("\n	at " + el[i].toString());
		}
		return sb.toString();
	}

	/**
	 * Este método garante a flexibilidade da utilização da múltiplas base de dados
	 * 
	 * @param nomeBanco - Nome do banco ao qual se deseja executar a procedure
	 * @return a chamada inicial a uma procedure no banco de dados utilizado
	 */
	public static String retornaChamadaProcedure(String nomeBanco, String nomeProcedure, int numParametros) {

		StringBuilder sb = new StringBuilder();
		String chamadaProcedure = "";

		if (nomeBanco.trim().equalsIgnoreCase("mysql")) {

			sb.append("call");
			sb.append(" ");
			sb.append(nomeProcedure);
			sb.append("(");

			for (int i = 0; i < numParametros; i++) {
				sb.append("?");
				sb.append(",");
			}

			sb.append(")");

			chamadaProcedure = sb.toString();

			chamadaProcedure = chamadaProcedure.replace(",)", ")");

		} else if (nomeBanco.trim().equalsIgnoreCase("sqlserver")) {

			sb.append("exec");
			sb.append(" ");
			sb.append(nomeProcedure);

		} else {

			sb.append("call");
			sb.append(" ");
			sb.append(nomeProcedure);
			sb.append("(");

			for (int i = 0; i < numParametros; i++) {
				sb.append("?");
				sb.append(",");
			}

			sb.append(")");

			chamadaProcedure = sb.toString();

			chamadaProcedure = chamadaProcedure.replace(",)", ")");

		}

		sb.append(" ");

		return chamadaProcedure;
	}

	/*
	 * Retorna o nome do Mês do Ano com base no código do mesmo
	 */
	public static String nomeMes(int month) {
		String monthString = "";

		switch (month) {
		case 0:
			monthString = "Janeiro";
			break;
		case 1:
			monthString = "Fevereiro";
			break;
		case 2:
			monthString = "Março";
			break;
		case 3:
			monthString = "Abril";
			break;
		case 4:
			monthString = "Maio";
			break;
		case 5:
			monthString = "Junho";
			break;
		case 6:
			monthString = "Julho";
			break;
		case 7:
			monthString = "Agosto";
			break;
		case 8:
			monthString = "Setembro";
			break;
		case 9:
			monthString = "Outubro";
			break;
		case 10:
			monthString = "Novembro";
			break;
		case 11:
			monthString = "Dezembro";
			break;
		}

		return monthString;
	}

	/*
	 * Consulta o E-mail de lideres de grupos
	 */
	public static String getMailLiderGrupo(Connection cnWF, String nomeGrupo) throws Exception {
		String retorno = "";

		StringBuilder sqlMailLider = new StringBuilder();
		sqlMailLider.append(" SELECT DES_EMAIL ");
		sqlMailLider.append(" 	FROM GRUPO G ");
		sqlMailLider.append("	JOIN USUARIO U ON ( U.COD_USUARIO = G.COD_USUARIO ) ");
		sqlMailLider.append("  WHERE G.DES_GRUPO = ");
		sqlMailLider.append(nomeGrupo);

		try (PreparedStatement pst = cnWF.prepareStatement(sqlMailLider.toString())) {

			try (ResultSet rs = pst.executeQuery()) {

				if (rs.next()) {
					retorno = rs.getString("DES_EMAIL");
				}
			}

		} catch (Exception e) {
			System.out.println("Erro:" + Funcoes.exceptionPrinter(e));
			throw e;
		}

		return retorno;
	}

	/**
	 * Metodo que executa uma consulta sql genï¿½rica e retorna um mapa com as
	 * informacoes
	 * 
	 * @param connection
	 * @param sql
	 * @param paramList
	 * @return
	 * @throws Exception
	 */
	public static List<Map<String, Object>> getGenericResultSet(Connection connection, String sql,
			List<Object> paramList) throws Exception {

		List<Map<String, Object>> returnList = new ArrayList<Map<String, Object>>();

		try (PreparedStatement pst = connection.prepareStatement(sql);) {
			int p = 1;

			for (Object param : paramList) {
				pst.setObject(p++, param);
			}

			try (ResultSet rs = pst.executeQuery();) {
				ResultSetMetaData metaData = rs.getMetaData();
				int columnCount = metaData.getColumnCount();

				while (rs.next()) {
					Map<String, Object> parametroMap = new HashMap<String, Object>();
					int columnIndex = 1;

					while (columnIndex < (columnCount + 1)) {
						parametroMap.put(metaData.getColumnName(columnIndex).toUpperCase(), rs.getObject(columnIndex));
						columnIndex++;
					}

					returnList.add(parametroMap);
				}
			}
		} catch (Exception e) {
			throw e;
		}

		return returnList;
	}

	public static Calendar getDataLimiteBasica(Calendar dataGravacao, String retornoSLA,
			List<String> diasNaoTrabalhadosList, Map<String, Map<String, String>> turnoSemanaMap) {
		Calendar dataSomada = Calendar.getInstance();
		dataSomada.setTime(dataGravacao.getTime());
		String[] retornoSLas = retornoSLA.split(";");
		long tempoAtraso = 0;
		int numero = Integer.parseInt(retornoSLas[0]);
		if (retornoSLas[1].equals("D")) {
			tempoAtraso = (numero * 8) * 3600;
		}
		if (retornoSLas[1].equals("H")) {
			tempoAtraso = (numero) * 3600;
		}

		Calendar dataLimite = new CalculaTempoLimite(dataGravacao, tempoAtraso, turnoSemanaMap, diasNaoTrabalhadosList)
				.getDataLimite();
		return dataLimite;
	}

	/**
	 * Calcula tempo limite da etapa em milissegundos quando o prazo limite ï¿½ uma
	 * data
	 * 
	 * @param dataGravacao
	 * @param dataLimite
	 * @param turnoSemanaMap
	 * @param diasNaoTrabalhadosList
	 * @return
	 * @throws Exception
	 */
	public static long getTempoLimiteInMillis(Calendar dataGravacao, Calendar dataLimite,
			Map<String, Map<String, String>> turnoSemanaMap, List<String> diasNaoTrabalhadosList) throws Exception {
		long retorno = 0;

		try {
			SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy");
			String sDataInicio = dateFormat.format(dataGravacao.getTime());
			String sDataFim = dateFormat.format(dataLimite.getTime());

			Calendar calInicioTurno = Calendar.getInstance();
			Calendar calFimTurno = Calendar.getInstance();

			Calendar dataAux = Calendar.getInstance();
			dataAux.setTimeInMillis(dataGravacao.getTimeInMillis());

			while (dataAux.compareTo(dataLimite) <= 0) {

				String sDataAux = dateFormat.format(dataAux.getTime());
				int diaSemana = dataAux.get(Calendar.DAY_OF_WEEK);

				Map<String, String> dia = turnoSemanaMap.get(String.valueOf(diaSemana));

				String[] aTurnoInicio = dia.get("HORA_ENTRADA").split("[:]");
				String[] aTurnoFim = dia.get("HORA_SAIDA").split("[:]");

				int turnoHoraInicio = Integer.parseInt(aTurnoInicio[0]);
				int turnoMinutoInicio = Integer.parseInt(aTurnoInicio[1]);
				int turnoHoraFim = Integer.parseInt(aTurnoFim[0]);
				int turnoMinutoFim = Integer.parseInt(aTurnoFim[1]);

				calFimTurno.setTimeInMillis(dataAux.getTimeInMillis());
				calFimTurno.set(Calendar.HOUR_OF_DAY, turnoHoraFim);
				calFimTurno.set(Calendar.MINUTE, turnoMinutoFim);
				calFimTurno.set(Calendar.SECOND, 0);
				calFimTurno.set(Calendar.MILLISECOND, 0);

				calInicioTurno.setTimeInMillis(dataAux.getTimeInMillis());
				calInicioTurno.set(Calendar.HOUR_OF_DAY, turnoHoraInicio);
				calInicioTurno.set(Calendar.MINUTE, turnoMinutoInicio);
				calInicioTurno.set(Calendar.SECOND, 0);
				calInicioTurno.set(Calendar.MILLISECOND, 0);

				// verifica se esta na lista de dias nï¿½o trabalhados
				if (!diasNaoTrabalhadosList.contains(sDataAux)) {

					// dia inicial
					if (sDataAux.equals(sDataInicio)) {
						if (dataAux.before(calFimTurno) && dataAux.after(calInicioTurno)) {
							retorno += (calFimTurno.getTimeInMillis() - dataAux.getTimeInMillis());
						}

						// dia final
					} else if (sDataAux.equals(sDataFim)) {
						if (dataAux.before(calFimTurno) && dataAux.after(calInicioTurno)) {
							retorno += (dataAux.getTimeInMillis() - calInicioTurno.getTimeInMillis());
						}

						// dias "normais"
					} else {
						retorno += (calFimTurno.getTimeInMillis() - calInicioTurno.getTimeInMillis());
					}
				}

				dataAux.add(Calendar.DAY_OF_MONTH, 1);
			}

		} catch (Exception e) {
			throw e;
		}
		return retorno;
	}

	/**
	 * Retorna um mapa com os horï¿½rios cadastrados de turnos (<dia>,
	 * <<hora_entrada>, <hora_saida>>)
	 * 
	 * @param connection
	 * @return
	 * @throws Exception
	 */
	public static Map<String, Map<String, String>> getTurnoSemanaMap(Connection connection) throws Exception {

		Map<String, Map<String, String>> turnoSemanaMap = new HashMap<String, Map<String, String>>();

		String sql = " select * from TURNO ";

		try (PreparedStatement pst = connection.prepareStatement(sql);) {

			try (ResultSet rs = pst.executeQuery();) {

				while (rs.next()) {
					Map<String, String> turnoDia = new HashMap<String, String>();

					// calcula e transforma registro em "hora:minuto"
					int inicio = rs.getInt("HORA_ENTRADA");
					int fim = rs.getInt("HORA_SAIDA");

					int horaIni = inicio / 60;
					String sHoraIni = (horaIni < 10) ? ("0" + horaIni) : ("" + horaIni);
					int minIni = inicio % 60;
					String sMinIni = (minIni < 10) ? ("0" + minIni) : ("" + minIni);

					int horaFim = fim / 60;
					String sHoraFim = (horaFim < 10) ? ("0" + horaFim) : ("" + horaFim);
					int minFim = fim % 60;
					String sMinFim = (minFim < 10) ? ("0" + minFim) : ("" + minFim);

					turnoDia.put("HORA_ENTRADA", sHoraIni + ":" + sMinIni);
					turnoDia.put("HORA_SAIDA", sHoraFim + ":" + sMinFim);

					turnoSemanaMap.put(rs.getString("COD_TURNO"), turnoDia);
				}
			}
		} catch (Exception e) {
			throw e;
		}

		return turnoSemanaMap;
	}

	public static Map<String, Map<String, String>> getTurnoSemanaMapBanco(Connection connection) throws Exception {

		Map<String, Map<String, String>> turnoSemanaMap = new HashMap<String, Map<String, String>>();

		String sql = " select * from TURNO ";

		try (PreparedStatement pst = connection.prepareStatement(sql);) {

			try (ResultSet rs = pst.executeQuery();) {

				while (rs.next()) {
					Map<String, String> turnoDia = new HashMap<String, String>();

					// calcula e transforma registro em "hora:minuto"
					int inicio = rs.getInt("HORA_ENTRADA");
					int fim = rs.getInt("HORA_SAIDA");

					turnoDia.put("HORA_ENTRADA", String.valueOf(inicio));
					turnoDia.put("HORA_SAIDA", String.valueOf(fim));

					turnoSemanaMap.put(rs.getString("COD_TURNO"), turnoDia);
				}
			}
		} catch (Exception e) {
			throw e;
		}

		return turnoSemanaMap;
	}

	/**
	 * Retorna todos os dias nï¿½o trabalhados
	 * 
	 * @param connection
	 * @return
	 * @throws Exception
	 */
	public static List<String> getDiasNaoTrabalhadosList(Connection connection) throws Exception {

		SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy");

		List<String> diasNaoTrabalhadosList = new ArrayList<String>();

		String sql1 = " select * from FERIADO ";
		String sql2 = " select * from FERIADO_ANUAL ";

		try (PreparedStatement pst1 = connection.prepareStatement(sql1); ResultSet rs1 = pst1.executeQuery();) {

			while (rs1.next()) {
				diasNaoTrabalhadosList.add(dateFormat.format(rs1.getDate("DATA")));
			}

			Calendar dataAtual = Calendar.getInstance();
			String sAno = String.valueOf(dataAtual.get(Calendar.YEAR));

			try (PreparedStatement pst2 = connection.prepareStatement(sql2); ResultSet rs2 = pst2.executeQuery();) {

				while (rs2.next()) {
					String sData = rs2.getString("DATA");
					String sMes = sData.substring(0, 2);
					String sDia = sData.substring(2);
					diasNaoTrabalhadosList.add(sDia + "/" + sMes + "/" + sAno);
				}
			}
		} catch (Exception e) {
			throw e;
		}

		return diasNaoTrabalhadosList;
	}

	/**
	 * Formata data YYYY-MM-DDTHH:mm:ss.sssZ ou DD/MM/YYYY em DD/MM/YYYY
	 * 
	 * @param string data
	 */
	public static String formatoDataDesconhecido(String dataDesconhecida) {

		if (dataDesconhecida != null && !"".equals(dataDesconhecida)) {

			if ("/".equals(dataDesconhecida.substring(2, 3))) {
				return dataDesconhecida;

			} else if (!"-".equals(dataDesconhecida.substring(4, 5))) {
				return dataDesconhecida;

			} else if ("-".equals(dataDesconhecida.substring(4, 5))) {
				return dataDesconhecida.substring(8, 10) + "/" + dataDesconhecida.substring(5, 7) + "/"
						+ dataDesconhecida.substring(0, 4);
			}

		} else {
			return "";
		}

		return "";
	}

	/**
	 * Funcao que remove acentos de um texto
	 * @param texto
	 * @return {@link String}
	 */
	public static String removeNaoNumericos(String texto){

		if(!texto.equals("")){
			String output = texto; 
			output = Normalizer.normalize(output, Normalizer.Form.NFD);  
			output = output.replaceAll("[^0-9]", "");

			return output;

		} else {
			return texto;
		}
	}

	/**
	 * Funcao para escrever por extenso valores numï¿½ricos
	 * @param valor. Ex : 55
	 * @return String. Ex: Cinquenta e cinco reais
	 */
	public static String valorPorExtenso(double vlr) {
		if (vlr == 0)
			return("zero");

		long inteiro = (long)Math.abs(vlr); // parte inteira do valor
		double resto = vlr - inteiro;       // parte fracionï¿½ria do valor

		String vlrS = String.valueOf(inteiro);
		if (vlrS.length() > 15)
			return("Erro: valor superior a 999 trilhï¿½es.");

		String s = "", saux, vlrP;
		String centavos = String.valueOf((int)Math.round(resto * 100));

		String[] unidade = {"", "um", "dois", "trï¿½s", "quatro", "cinco",
				"seis", "sete", "oito", "nove", "dez", "onze",
				"doze", "treze", "quatorze", "quinze", "dezesseis",
				"dezessete", "dezoito", "dezenove"};
		String[] centena = {"", "cento", "duzentos", "trezentos",
				"quatrocentos", "quinhentos", "seiscentos",
				"setecentos", "oitocentos", "novecentos"};
		String[] dezena = {"", "", "vinte", "trinta", "quarenta", "cinquenta",
				"sessenta", "setenta", "oitenta", "noventa"};
		String[] qualificaS = {"", "mil", "milhï¿½o", "bilhï¿½o", "trilhï¿½o"};
		String[] qualificaP = {"", "mil", "milhï¿½es", "bilhï¿½es", "trilhï¿½es"};

		// definindo o extenso da parte inteira do valor
		int n, unid, dez, cent, tam, i = 0;
		boolean umReal = false, tem = false;
		while (!vlrS.equals("0")) {
			tam = vlrS.length();
			// retira do valor a 1a. parte, 2a. parte, por exemplo, para 123456789:
			// 1a. parte = 789 (centena)
			// 2a. parte = 456 (mil)
			// 3a. parte = 123 (milhï¿½es)
			if (tam > 3) {
				vlrP = vlrS.substring(tam-3, tam);
				vlrS = vlrS.substring(0, tam-3);
			}
			else { // ï¿½ltima parte do valor
				vlrP = vlrS;
				vlrS = "0";
			}
			if (!vlrP.equals("000")) {
				saux = "";
				if (vlrP.equals("100"))
					saux = "cem";
				else {
					n = Integer.parseInt(vlrP, 10);  // para n = 371, tem-se:
					cent = n / 100;                  // cent = 3 (centena trezentos)
					dez = (n % 100) / 10;            // dez  = 7 (dezena setenta)
					unid = (n % 100) % 10;           // unid = 1 (unidade um)
					if (cent != 0)
						saux = centena[cent];
					if ((n % 100) <= 19) {
						if (saux.length() != 0)
							saux = saux + " e " + unidade[n % 100];
						else saux = unidade[n % 100];
					}
					else {
						if (saux.length() != 0)
							saux = saux + " e " + dezena[dez];
						else saux = dezena[dez];
						if (unid != 0) {
							if (saux.length() != 0)
								saux = saux + " e " + unidade[unid];
							else saux = unidade[unid];
						}
					}
				}
				if (vlrP.equals("1") || vlrP.equals("001")) {
					if (i == 0) // 1a. parte do valor (um real)
						umReal = true;
					else saux = saux + " " + qualificaS[i];
				}
				else if (i != 0)
					saux = saux + " " + qualificaP[i];
				if (s.length() != 0)
					s = saux + ", " + s;
				else s = saux;
			}
			if (((i == 0) || (i == 1)) && s.length() != 0)
				tem = true; // tem centena ou mil no valor
			i = i + 1; // prï¿½ximo qualificador: 1- mil, 2- milhï¿½o, 3- bilhï¿½o, ...
		}

		if (s.length() != 0) {
			if (umReal)
				s = s + " real";
			else if (tem)
				s = s + " reais";
			else s = s + " de reais";
		}

		// definindo o extenso dos centavos do valor
		if (!centavos.equals("0")) { // valor com centavos
			if (s.length() != 0) // se nï¿½o ï¿½ valor somente com centavos
				s = s + " e ";
			if (centavos.equals("1"))
				s = s + "um centavo";
			else {
				n = Integer.parseInt(centavos, 10);
				if (n <= 19)
					s = s + unidade[n];
				else {             // para n = 37, tem-se:
					unid = n % 10;   // unid = 37 % 10 = 7 (unidade sete)
					dez = n / 10;    // dez  = 37 / 10 = 3 (dezena trinta)
					s = s + dezena[dez];
					if (unid != 0)
						s = s + " e " + unidade[unid];
				}
				s = s + " centavos";
			}
		}

		return(s);
	}

	public static Integer getVersaoForm(Connection connection, Integer codProcesso, Logger logger) throws Exception { 
		
		Integer ret = 0;

		StringBuilder sql = new StringBuilder();
		sql.append(" SELECT COD_VERSAO FROM PROCESSO ");
		sql.append("  WHERE COD_PROCESSO = ? ");

		try (PreparedStatement pst = connection.prepareStatement(sql.toString());) {
			pst.setInt(1, codProcesso);

			try (ResultSet rs = pst.executeQuery();) {

				if(rs.next()){
					ret = rs.getInt("COD_VERSAO");
				}
			}

		} catch (Exception e) {
			throw e;
		}

		return ret;
	}

	/**
	 * Funcao que remove acentos de um texto
	 * @param texto
	 * @return {@link String}
	 */
	public static String removeAcentosEspaco(String texto){

		if(!texto.equals("")){
			String output = texto; 
			output = Normalizer.normalize(output, Normalizer.Form.NFD);  
			output = output.replaceAll("[^\\p{ASCII}]", "");
			output = output.replaceAll("[\\s]", "");
			output = output.replaceAll("[)]", "");
			output = output.replaceAll("[(]", "");
			output = output.replaceAll("[-]", "");
			output = output.replaceAll("[.]", "");
			output = output.replaceAll("[/]", "_");
			return output;

		} else {
			return texto;
		}
	}

	/**
	 * Recupera os dados da tabela PROCESSO, com base no codigo, retornando um Map<coluna, valor>
	 * @param connection
	 * @param codProcesso
	 * @return
	 * @throws Exception
	 */
	public static Map<String, String> getDadosAtualProcesso(Connection connection, Integer codProcesso) throws Exception {

		Map<String, String> parametroMap = new HashMap<String, String>();

		StringBuilder sql = new StringBuilder();
		sql.append(" SELECT * ");
		sql.append(" FROM PROCESSO ");
		sql.append(" WHERE COD_PROCESSO = ? ");

		try (PreparedStatement pst = connection.prepareStatement(sql.toString())) {
			pst.setInt(1, codProcesso);

			try (ResultSet rs = pst.executeQuery();) {
				ResultSetMetaData metaData = rs.getMetaData();
				int columnCount = metaData.getColumnCount();

				if (rs.next()) {
					int columnIndex = 1;
					while(columnIndex < (columnCount + 1)) {
						parametroMap.put(metaData.getColumnName(columnIndex).toUpperCase(), nulo(rs.getString(columnIndex), ""));
						columnIndex++;
					}
				}
			}
		} catch (Exception e) {
			throw e;
		}

		return parametroMap;
	}

	/**
	 * Recupera o status da etapa
	 * @param cnwf
	 * @param codProcesso
	 * @param codEtapa
	 * @param codCiclo
	 * @return
	 * @throws Exception
	 */
	public static String getStatusEtapa(Connection cnwf, int codProcesso, int codEtapa, int codCiclo) throws Exception {

		String retorno = "";

		StringBuilder sql =	new StringBuilder();
		sql.append(" SELECT IDE_STATUS ");
		sql.append("   FROM PROCESSO_ETAPA ");
		sql.append("  WHERE COD_PROCESSO = ? ");
		sql.append("	AND COD_ETAPA = ? ");
		sql.append("	AND COD_CICLO = ? ");

		try (PreparedStatement pst = cnwf.prepareStatement(sql.toString());) {
			pst.setInt(1, codProcesso);
			pst.setInt(2, codEtapa);
			pst.setInt(3, codCiclo);

			try (ResultSet rs = pst.executeQuery();) {
				if (rs.next()) {
					retorno = rs.getString("IDE_STATUS");
				}
			}
		} catch (Exception e) {
			throw e;
		}

		return retorno;
	}


	/**
	 * Obtem o código da versão ativa do formulário a partir do código do formulário
	 * @param connection
	 * @param codForm
	 * @return
	 * @throws Exception 
	 */
	private String getCodVersaoFormAtivo(Connection connection, Logger logger, String codForm) throws Exception {
		String codVersao = "0";

		StringBuilder sql = new StringBuilder();

		sql.append(" SELECT ");
		sql.append("	MAX(COD_VERSAO) COD_VERSAO ");
		sql.append(" FROM ");
		sql.append("	FORMULARIO ");
		sql.append(" WHERE ");
		sql.append("	COD_FORM = ? ");
		sql.append("	AND UPPER(IDE_STATUS) = 'A' ");

		try (PreparedStatement pst = connection.prepareStatement(sql.toString())) {
			pst.setString(1, codForm);

			try (ResultSet rs = pst.executeQuery()) {
				if (rs.next()) {
					codVersao = rs.getString("COD_VERSAO");
				}
			}
		} catch (Exception e) {
			logger.error("ERRO AO CONSULTAR A VERSÃO DO FORMULARIO: ", e);
			throw e;
		}

		return codVersao;
	}


	/**
	 * Verifica se o processo esta em modo teste
	 * @param connection
	 * @param codForm
	 * @param codVersao
	 * @return
	 * @throws Exception 
	 */
	private String getModoTesteForm(Connection connection, Logger logger, String codForm, String codVersao) throws Exception {
		String modoTeste = "false";

		StringBuilder sql = new StringBuilder();

		sql.append(" SELECT ");
		sql.append("	IDE_BETA_TESTE ");
		sql.append(" FROM ");
		sql.append("	FORMULARIO ");
		sql.append(" WHERE ");
		sql.append("	COD_FORM = ? ");
		sql.append("	AND COD_VERSAO = ? ");

		try (PreparedStatement pst = connection.prepareStatement(sql.toString())) {
			pst.setString(1, codForm);
			pst.setString(2, codVersao);

			try (ResultSet rs = pst.executeQuery()) {
				if (rs.next()) {
					String betaTeste = rs.getString("IDE_BETA_TESTE");

					if (betaTeste.equalsIgnoreCase("S")) {
						modoTeste = "true";
					}
				}
			}
		} catch (Exception e) {
			logger.error("ERRO AO CONSULTAR SE O PROCESSO ESTA EM MODO TESTE: ", e);
			throw e;
		}

		return modoTeste;
	}


	/**
	 * Obtem o código da ciclo da etapa a partir do código da etapa e processo
	 * @param connection
	 * @param codProcesso
	 * @param codEtapa
	 * @return
	 * @throws Exception 
	 */
	private String getMaxCodCicloProcesso(Connection connection, Logger logger, String codProcesso, String codEtapa) throws Exception {
		String codCiclo = "";

		StringBuilder sql = new StringBuilder();

		sql.append(" SELECT ");
		sql.append("	MAX(COD_CICLO) COD_CICLO ");
		sql.append(" FROM ");
		sql.append("	PROCESSO_ETAPA ");
		sql.append(" WHERE ");
		sql.append("	COD_PROCESSO = ? ");
		sql.append("	AND COD_ETAPA = ? ");
		sql.append("	AND IDE_STATUS = 'A' ");

		try (PreparedStatement pst = connection.prepareStatement(sql.toString())) {
			pst.setString(1, codProcesso);
			pst.setString(2, codEtapa);

			try (ResultSet rs = pst.executeQuery()) {

				if (rs.next()) {

					codCiclo = Funcoes.nulo(rs.getString("COD_CICLO"), "");
					logger.debug("Entrou select " + codCiclo);
				}
			}
		} catch (Exception e) {
			logger.error("ERRO AO CONSULTAR A CICLO DA ETAPA: ", e);
			throw e;
		}

		return codCiclo;
	}

}
