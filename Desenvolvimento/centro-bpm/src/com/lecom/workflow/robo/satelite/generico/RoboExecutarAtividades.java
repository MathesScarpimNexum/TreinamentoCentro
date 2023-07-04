package com.lecom.workflow.robo.satelite.generico;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;

import com.lecom.tecnologia.db.DBUtils;
import com.lecom.workflow.cadastros.common.util.Funcoes;
import com.lecom.workflow.cadastros.rotas.AprovaProcesso;
import com.lecom.workflow.cadastros.rotas.CancelarProcesso;
import com.lecom.workflow.cadastros.rotas.LoginAutenticacao;
import com.lecom.workflow.cadastros.rotas.exception.CancelaProcessoException;
import com.lecom.workflow.cadastros.rotas.exception.LoginAuthenticationException;
import com.lecom.workflow.cadastros.rotas.util.DadosLogin;
import com.lecom.workflow.cadastros.rotas.util.DadosProcesso;
import com.lecom.workflow.cadastros.rotas.util.DadosProcessoAbertura;
import com.lecom.workflow.robo.RBOpenWebServices;
import com.lecom.workflow.robo.face.GenericWSVO;
import com.lecom.workflow.robo.face.WebServices;
import com.lecom.workflow.robo.face.WebServicesVO;

import br.com.lecom.atos.servicos.annotation.Execution;
import br.com.lecom.atos.servicos.annotation.RobotModule;
import br.com.lecom.atos.servicos.annotation.Version;

@RobotModule(value = "RoboExecutarAtividades")
@Version({1,0,14})
public class RoboExecutarAtividades implements WebServices {
	
	private static Logger logger = Logger.getLogger(RoboExecutarAtividades.class);
	private String configPath = Funcoes.getWFRootDir() + "/upload/cadastros/config/";
	
	private String accessToken530SSO;
	
	@Execution
	public void AprovarEtapas() throws Exception{
		
		logger.info(">> Inicio RoboExecutarAtividades Genrico <<");
		
		try ( Connection cnLecom = DBUtils.getConnection("workflow") ) {
			
			Map<String, String> parametros = Funcoes.getParametrosIntegracao(configPath + getClass().getSimpleName());
			
			Map<String, String> automatico = Funcoes.getParametrosIntegracao(configPath + "automatico");
			Integer codUsuarioAutomatico = new Integer ( automatico.get("codUsuarioAutomatico") );
			String loginUsuarioAutomatico = automatico.get("loginUsuarioAutomatico");
			String senhaUsuarioAutomatico = automatico.get("senhaUsuarioAutomatico");
			String UrlSSo = automatico.get("enderecoSSO");
			String urlBPM = automatico.get("enderecoBPM");
			
			// Data Atual
			Calendar datAtual = Calendar.getInstance();
			
			// Cria um Objeto LocalDate com a data atual ( Java 8 ).
	        LocalDate dataAtual = LocalDate.now();

			// Aprova etapas, em paralelo, definidas no propertie, paradas com o Rob
			executarAtividadeConcentradora(cnLecom, false, parametros.get("aprovacoesParalelas").trim(), codUsuarioAutomatico, loginUsuarioAutomatico, senhaUsuarioAutomatico, "P", null, false);
			
			// Aprova etapas, definidas no propertie, paradas com o Rob
			executarAtividades(cnLecom, false, parametros.get("aprovacoes"), codUsuarioAutomatico, loginUsuarioAutomatico, senhaUsuarioAutomatico, "P", null, false);
			
			// Rejeita etapas, definidas no propertie, paradas com o Rob
			executarAtividades(cnLecom, false, parametros.get("rejeicoes"), codUsuarioAutomatico, loginUsuarioAutomatico, senhaUsuarioAutomatico, "R", null, false);
			
			// Aprova etapas em uma data determinada por um campo, definido no propertie, que estejam paradas com o Rob
			executarAtividadesDataCampo(cnLecom, false, datAtual, parametros.get("aprovacoesDataCampo"), codUsuarioAutomatico, loginUsuarioAutomatico, senhaUsuarioAutomatico, "P", null, false);
			
			// Rejeita etapas em uma data determinada por um campo, definido no propertie, que estejam paradas com o Rob
			executarAtividadesDataCampo(cnLecom, false, datAtual, parametros.get("rejeicoesDataCampo"), codUsuarioAutomatico, loginUsuarioAutomatico, senhaUsuarioAutomatico, "R", null, false);
			
			// Aprova atividades que o "prazo mximo", definido para ela, tenha excedido e que estejam paradas com o rob
			executarAtividadesPrazoMaximoExcedido(cnLecom, false, datAtual, parametros.get("aprovacoesEtapasPrazoMaximoExcedido"), codUsuarioAutomatico, loginUsuarioAutomatico, senhaUsuarioAutomatico, "P", null, false);
			
			// Rejeita atividades que o "prazo mximo", definido para ela, tenha excedido e que estejam paradas com o rob
			executarAtividadesPrazoMaximoExcedido(cnLecom, false, datAtual, parametros.get("rejeicoesEtapasPrazoMaximoExcedido"), codUsuarioAutomatico, loginUsuarioAutomatico, senhaUsuarioAutomatico, "R", null, false);
			
			
			// ************************************************* Form Novo *************************************************
			
			// Gerar Access Token SSO
			gerarAccessToken530SSO(cnLecom, loginUsuarioAutomatico, senhaUsuarioAutomatico, UrlSSo);
			
			logger.info("accessToken530SSO = "+accessToken530SSO);
			// Caso no seja gerado o token no executa as chamadas aos servios.
			if ( accessToken530SSO != null && !"".equalsIgnoreCase( accessToken530SSO ) ) {
				
				// Aprova etapas, em paralelo, definidas no propertie, paradas com o Rob
				executarAtividadeConcentradora(cnLecom, true, parametros.get("aprovacoesFNConcentradora").trim(), codUsuarioAutomatico, loginUsuarioAutomatico, senhaUsuarioAutomatico, "P", urlBPM, false);
				// Aprova etapas, em paralelo, definidas no propertie, paradas com Usurio Comum
				executarAtividadeConcentradora(cnLecom, true, parametros.get("aprovacoesFNConcentradoraUC").trim(), codUsuarioAutomatico, loginUsuarioAutomatico, senhaUsuarioAutomatico, "P", urlBPM, true);
				
				// Aprova etapas, em paralelo, definidas no propertie, paradas com o Rob
				executarAtividadeConcentradora(cnLecom, true, parametros.get("rejeicoesFNConcentradora"), codUsuarioAutomatico, loginUsuarioAutomatico, senhaUsuarioAutomatico, "R", urlBPM, false);
				// Aprova etapas, em paralelo, definidas no propertie, paradas com Usurio Comum
				executarAtividadeConcentradora(cnLecom, true, parametros.get("rejeicoesFNConcentradoraUC"), codUsuarioAutomatico, loginUsuarioAutomatico, senhaUsuarioAutomatico, "R", urlBPM, true);
				
				// Aprova etapas, definidas no propertie, paradas com o Rob
				executarAtividades(cnLecom, true, parametros.get("aprovacoesFN"), codUsuarioAutomatico, loginUsuarioAutomatico, senhaUsuarioAutomatico, "P", urlBPM, false);
				// Aprova etapas, definidas no propertie, paradas com Usurio Comum
				executarAtividades(cnLecom, true, parametros.get("aprovacoesFNUC"), codUsuarioAutomatico, loginUsuarioAutomatico, senhaUsuarioAutomatico, "P", urlBPM, true);
				
				// Rejeita etapas, definidas no propertie, paradas com o Rob
				executarAtividades(cnLecom, true, parametros.get("rejeicoesFN"), codUsuarioAutomatico, loginUsuarioAutomatico, senhaUsuarioAutomatico, "R", urlBPM, false);
				// Rejeita etapas, definidas no propertie, paradas com Usurio Comum
				executarAtividades(cnLecom, true, parametros.get("rejeicoesFNUC"), codUsuarioAutomatico, loginUsuarioAutomatico, senhaUsuarioAutomatico, "R", urlBPM, true);
				
				// Aprova etapas em uma data determinada por um campo, definido no propertie, que estejam paradas com o Rob
				executarAtividadesDataCampo(cnLecom, true, datAtual, parametros.get("aprovacoesFNDataCampo"), codUsuarioAutomatico, loginUsuarioAutomatico, senhaUsuarioAutomatico, "P", urlBPM, false);
				// Aprova etapas em uma data determinada por um campo, definido no propertie, que estejam paradas com Usurio Comum
				executarAtividadesDataCampo(cnLecom, true, datAtual, parametros.get("aprovacoesFNDataCampoUC"), codUsuarioAutomatico, loginUsuarioAutomatico, senhaUsuarioAutomatico, "P", urlBPM, true);
				
				// Rejeita etapas em uma data determinada por um campo, definido no propertie, que estejam paradas com o Rob
				executarAtividadesDataCampo(cnLecom, true, datAtual, parametros.get("rejeicoesFNDataCampo"), codUsuarioAutomatico, loginUsuarioAutomatico, senhaUsuarioAutomatico, "R", urlBPM, false);
				// Rejeita etapas em uma data determinada por um campo, definido no propertie, que estejam paradas com Usurio Comum
				executarAtividadesDataCampo(cnLecom, true, datAtual, parametros.get("rejeicoesFNDataCampoUC"), codUsuarioAutomatico, loginUsuarioAutomatico, senhaUsuarioAutomatico, "R", urlBPM, true);
								
				// Aprova atividades que o "prazo mximo", definido para ela, tenha excedido e que estejam paradas com o rob
				executarAtividadesPrazoMaximoExcedido(cnLecom, true, datAtual, parametros.get("aprovacoesFNEtapasPrazoMaximoExcedido"), codUsuarioAutomatico, loginUsuarioAutomatico, senhaUsuarioAutomatico, "P", urlBPM, false);
				// Aprova atividades que o "prazo mximo", definido para ela, tenha excedido e que estejam paradas com Usurio Comum
				executarAtividadesPrazoMaximoExcedido(cnLecom, true, datAtual, parametros.get("aprovacoesFNEtapasPrazoMaximoExcedidoUC"), codUsuarioAutomatico, loginUsuarioAutomatico, senhaUsuarioAutomatico, "P", urlBPM, true);
				
				// Rejeita atividades que o "prazo mximo", definido para ela, tenha excedido e que estejam paradas com o rob
				executarAtividadesPrazoMaximoExcedido(cnLecom, true, datAtual, parametros.get("rejeicoesFNEtapasPrazoMaximoExcedido"), codUsuarioAutomatico, loginUsuarioAutomatico, senhaUsuarioAutomatico, "R", urlBPM, false);
				// Rejeita atividades que o "prazo mximo", definido para ela, tenha excedido e que estejam paradas com Usurio Comum
				executarAtividadesPrazoMaximoExcedido(cnLecom, true, datAtual, parametros.get("rejeicoesFNEtapasPrazoMaximoExcedidoUC"), codUsuarioAutomatico, loginUsuarioAutomatico, senhaUsuarioAutomatico, "R", urlBPM, true);
				
				// Cancela processos que esto na atividade inicial, ciclo 1, parados a mais de x dias
				cancelaProcessoNaoEnviado(cnLecom, dataAtual, parametros.get("cancelaFNProcessoNaoEnviado"), codUsuarioAutomatico, urlBPM, false);
				// Cancela processos que esto na atividade inicial, ciclo 1, parados a mais de x dias, com Usurio Comum
				cancelaProcessoNaoEnviado(cnLecom, dataAtual, parametros.get("cancelaFNProcessoNaoEnviadoUC"), codUsuarioAutomatico, urlBPM, true);
			}
			
		} catch (Exception e) {
			logger.error("[ ERRO ] : ", e);
		}
		
		logger.info(">> Fim RoboExecutarAtividades Genrico <<");
	}
	
	// Executar atividades do form 5.20
	private String executaEtapaProcesso( int codForm, int codProcesso, int codEtapa, int codCiclo, String valores, String paramAcaoAprovaRejeita, String loginRobo, String senhaRobo ) throws Exception {
		
		logger.info("INICIO executaEtapaProcesso");
		logger.info("VALORES ( codForm : " + codForm + " / codProcesso : " + codProcesso + " / codEtapa : " + codEtapa + " / codCiclo : " + codCiclo + " / valores : " + valores + " / acao : " + paramAcaoAprovaRejeita);// + " / loginRobo : " + loginRobo + " / senhaRobo : " + senhaRobo);
		
		GenericWSVO webServicesVO = new GenericWSVO();
		webServicesVO.setCodForm(codForm);
		webServicesVO.setCodProcesso(codProcesso);
		webServicesVO.setCodEtapa(codEtapa);
		webServicesVO.setCodCiclo(codCiclo);	
		webServicesVO.setLoginUsuario(loginRobo);
		webServicesVO.setSenhaUsuario(senhaRobo);		
		webServicesVO.setAcao(paramAcaoAprovaRejeita);
		webServicesVO.setValores(valores);
		
		String retorno = RBOpenWebServices.getInstance().executeWebServices(webServicesVO, RBOpenWebServices.EXECUTA_ETAPA_PROCESSO);
		
		logger.info("FIM executaEtapaProcesso");
		
		return retorno;
	}
	
	/**
	 * Metodo que chama o servio de execuo de atividades do form novo 5.30
	 * @param String codProcessoExecutar - Obrigatrio
	 * @param String codAtividadeExecutar - Obrigatrio
	 * @param String codCicloExecutar - Obrigatrio
	 * @param String modoTeste - Obrigatrio
	 * @param Map<String, String> camposValores - No Obrigatrio - Onde MAP deve conter nome do campo e valores
	 * @param Map<String, List<Map<String, Object>>> gridValores - No Obrigatrio - Onde MAP deve conter o nome da grid e um LIST<MAP> que conter os nomes dos campos e valores
	 * @param Integer codUsuarioAutomatico - Obrigatrio
	 * @param String loginUsuarioAutomatico - Obrigatrio
	 * @param String senhaUsuarioAutomatico - Obrigatrio
	 * @param String paramAcaoAprovaRejeita - Obrigatrio - Onde P = Aprovar, R = Rejeitar
	 * @return String retornoAprovacao
	 * @throws Exception
	 */
	private String executaAtividProcessoFormNovo( String codProcessoExecutar, String codAtividadeExecutar, String codCicloExecutar, String modoTeste, 
												  Map<String,String> camposValores, Map<String,List<Map<String, Object>>> gridValores, Integer codUsuarioAutomatico, 
												  String loginUsuarioAutomatico, String senhaUsuarioAutomatico, String paramAcaoAprovaRejeita, String urlBPM ) throws Exception {
		
		logger.info("INICIO executaAtividProcessoFormNovo");
		logger.info("VALORES ( AccessToken530SSO : " + accessToken530SSO + " / codProcessoExecutar : " + codProcessoExecutar + " / codAtividadeExecutar : " + codAtividadeExecutar + " / codCicloExecutar : " + codCicloExecutar + " / modoTeste : " + modoTeste + " / paramAcaoAprovaRejeita : " + paramAcaoAprovaRejeita);
		logger.info("VALORES ( codUsuarioAutomatico : " + codUsuarioAutomatico + " / loginUsuarioAutomatico : " + loginUsuarioAutomatico + " / senhaUsuarioAutomatico : " + senhaUsuarioAutomatico);
		
		String retornoAprovacao = "";
		
		try {
			
			DadosProcesso dadosProcesso = new DadosProcesso(paramAcaoAprovaRejeita);
			
			if ( camposValores != null ) {
			dadosProcesso.geraPadroes(camposValores);
			}
			
			if ( gridValores != null ) {
				gridValores.forEach((nomeGrid, valores) -> dadosProcesso.geraValoresGrid(nomeGrid, valores) );
			}
			
			DadosProcessoAbertura procOrigemUtil = new DadosProcessoAbertura();
			procOrigemUtil.setProcessInstanceId(codProcessoExecutar);
			procOrigemUtil.setCurrentActivityInstanceId(codAtividadeExecutar);
			procOrigemUtil.setCurrentCycle(codCicloExecutar);
			procOrigemUtil.setModoTeste(modoTeste.equals("S") ? "true" : "false");
			
			logger.debug("procOrigemUtil info: " + procOrigemUtil.getCurrentActivityInstanceId() + ", " + procOrigemUtil.getCurrentCycle() + ", " + procOrigemUtil.getProcessInstanceId() + " modoTeste= "+procOrigemUtil.getModoTeste()+" | usuario: " + codUsuarioAutomatico);
			AprovaProcesso aprovaProcesso = new AprovaProcesso( urlBPM, accessToken530SSO, procOrigemUtil, dadosProcesso, procOrigemUtil.getModoTeste(), codUsuarioAutomatico.toString() );
			retornoAprovacao = aprovaProcesso.aprovaProcesso();
			logger.debug("retornoAprovacao = " + retornoAprovacao);

		} catch (Exception e) {
			logger.error("executaAtividProcessoFormNovo : ", e);
			retornoAprovacao = Funcoes.exceptionPrinter(e);
		}

		logger.info("FIM executaAtividProcessoFormNovo");
		
		return retornoAprovacao;
	}
	
	
	private String cancelaProcessoFormNovo( String codProcessoExecutar, String codAtividadeExecutar, String codCicloExecutar, String modoTeste, 
			  								Integer codUsuarioAutomatico, String urlBPM ) throws Exception {
		
		String retornoCancelamento = "falha"; 
		
		try {
			
			logger.info("INICIO retornoCancelamento");

			DadosProcessoAbertura procOrigemUtil = new DadosProcessoAbertura();
			procOrigemUtil.setProcessInstanceId(codProcessoExecutar);
			procOrigemUtil.setCurrentActivityInstanceId(codAtividadeExecutar);
			procOrigemUtil.setCurrentCycle(codCicloExecutar);
			procOrigemUtil.setModoTeste(modoTeste.equals("S") ? "true" : "false");

			CancelarProcesso cancelar = new CancelarProcesso(urlBPM, accessToken530SSO, procOrigemUtil, procOrigemUtil.getModoTeste(), codUsuarioAutomatico.toString());

			retornoCancelamento = cancelar.cancelarProcesso();
			logger.debug("retornoCancelamento = " + retornoCancelamento);
			
		} catch (CancelaProcessoException e1) {
			logger.error("[ ERRO CancelaProcessoException ] : ", e1);
		}
		
		logger.info("FIM retornoCancelamento");
		return retornoCancelamento;
	}
	
	
	/*
	 * Aprova etapas, em paralelo, definidas no propertie, paradas com o Rob
	 */
	private void executarAtividadeConcentradora( Connection cnLecom, boolean formNovo, String atividadesExecutar, Integer codUsuarioAutomatico, String loginUsuarioAutomatico, 
												 String senhaUsuarioAutomatico, String paramAcaoAprovaRejeita, String urlBPM, boolean transferirRobo ) throws Exception{
		
		logger.info("INICIO executaEtapaConcentradora : " + atividadesExecutar);
		
		if(atividadesExecutar != null && !"".equals(atividadesExecutar) ){
		
			for (String atividadeExecut : atividadesExecutar.split(";")) {
				
				String[] aprovacaoFormEtapa = atividadeExecut.split("@");
				String[] aprovacaoEtapasConcentradora = aprovacaoFormEtapa[1].split("-");
				
				String codFormConcentradora = aprovacaoFormEtapa[0];
				String codEtapaConcentradora = aprovacaoEtapasConcentradora[0];
				String codEtapasParalelismo = aprovacaoEtapasConcentradora[1];
				
				logger.info("PARALELO - Em analise - Form : " + codFormConcentradora + " / Etapa Concentradora : " + codEtapaConcentradora + " / Etapa Paralelismo : " + codEtapasParalelismo);
	
				// Consulta as etapas Concentradoras que ainda esto em aberto 
				StringBuilder sqlConsultaAux1 = new StringBuilder();
				sqlConsultaAux1.append(" 	 SELECT P.COD_FORM, PE.COD_PROCESSO, PE.COD_ETAPA, PE.COD_CICLO, P.IDE_BETA_TESTE ");
				sqlConsultaAux1.append(" 	   FROM PROCESSO_ETAPA PE ");
				sqlConsultaAux1.append(" INNER JOIN PROCESSO P ON ( P.COD_PROCESSO = PE.COD_PROCESSO ) ");
				sqlConsultaAux1.append(" 	  WHERE PE.IDE_STATUS IN ('A') ");
				sqlConsultaAux1.append(" 		AND P.COD_FORM = ");
				sqlConsultaAux1.append(codFormConcentradora);
				sqlConsultaAux1.append(" 		AND PE.COD_ETAPA IN ( ");
				sqlConsultaAux1.append(codEtapaConcentradora);
				sqlConsultaAux1.append(" ) ");
				
				if ( !transferirRobo ) {
					sqlConsultaAux1.append(" 		  AND PE.COD_USUARIO_ETAPA IN ( ");
					sqlConsultaAux1.append(codUsuarioAutomatico);
					sqlConsultaAux1.append(" ) ");
				}
				
				sqlConsultaAux1.append("   ORDER BY PE.COD_PROCESSO, PE.COD_ETAPA, PE.COD_CICLO ");
	
				try (PreparedStatement pstConsultaAux1 = cnLecom.prepareStatement(sqlConsultaAux1.toString());
						 ResultSet rsConsultaAux1 = pstConsultaAux1.executeQuery();) {
				
					while (rsConsultaAux1.next()) {
						
						// Consulta as etapas do Paralelismo que ainda esto em aberto
						StringBuilder sqlConsultaAux2 = new StringBuilder();
						sqlConsultaAux2.append(" 	 SELECT COUNT(P.COD_PROCESSO) QTD_ETAPAS_ABERTO ");
						sqlConsultaAux2.append(" 	   FROM PROCESSO_ETAPA PE ");
						sqlConsultaAux2.append(" INNER JOIN PROCESSO P ON ( P.COD_PROCESSO = PE.COD_PROCESSO ) ");
						sqlConsultaAux2.append(" 	  WHERE PE.IDE_STATUS IN ('A') ");
						sqlConsultaAux2.append(" 		AND P.COD_PROCESSO = ");
						sqlConsultaAux2.append(rsConsultaAux1.getString("COD_PROCESSO"));
						sqlConsultaAux2.append(" 		AND PE.COD_ETAPA IN ( ");
						sqlConsultaAux2.append(codEtapasParalelismo);
						sqlConsultaAux2.append(" ) ");
//						sqlConsultaAux2.append("   ORDER BY PE.COD_PROCESSO, PE.COD_ETAPA, PE.COD_CICLO ");
		
						try (PreparedStatement pstConsultaAux2 = cnLecom.prepareStatement(sqlConsultaAux2.toString());
								 ResultSet rsConsultaAux2 = pstConsultaAux2.executeQuery();) {
						
							while (rsConsultaAux2.next()) {
								
								if (rsConsultaAux2.getInt("QTD_ETAPAS_ABERTO") == 0) {
									
									Integer codProcesso = rsConsultaAux1.getInt("COD_PROCESSO");
									Integer codEtapa = rsConsultaAux1.getInt("COD_ETAPA");
									Integer codCiclo = rsConsultaAux1.getInt("COD_CICLO");
									String modoTeste = rsConsultaAux1.getString("IDE_BETA_TESTE");
									
									if ( transferirRobo ) {
							    		if ( !verificaUsuarioProcessoEtapaUsu( cnLecom, codProcesso, codEtapa, codCiclo, codUsuarioAutomatico ) ) {
							    			inserirUsuarioEtapa ( cnLecom, codProcesso, codEtapa, codCiclo, codUsuarioAutomatico );
							    		}
									}
									
									if ( formNovo ) {
										logger.info("APROVACOES - RETORNO FN : Proc / Etapa - ( " + codProcesso + " / " + codEtapa + " ) - " + executaAtividProcessoFormNovo(codProcesso.toString(), codEtapa.toString(), codCiclo.toString(), modoTeste, null, null, codUsuarioAutomatico, loginUsuarioAutomatico, senhaUsuarioAutomatico, paramAcaoAprovaRejeita, urlBPM));
									
									} else {
//										String valores = "OBSERVACAO|Processo aprovado automaticamente.";
										String valores = "";
										logger.info("PARALELO - RETORNO : Proc / Etapa - ( " + codProcesso + " / " + codEtapa + " ) - " + executaEtapaProcesso(rsConsultaAux1.getInt("COD_FORM"), codProcesso, codEtapa, codCiclo, valores, paramAcaoAprovaRejeita, loginUsuarioAutomatico, senhaUsuarioAutomatico));
									}
									
								}
							}
						}
					}
				}
			}
		}
		
		logger.info("FIM executaEtapaConcentradora");
	}
	
	
	/*
	 * Aprova/Rejeita etapas, definidas no propertie, paradas com o Rob
	 */
	private void executarAtividades( Connection cnLecom, boolean formNovo, String atividadesExecutar, Integer codUsuarioAutomatico, String loginUsuarioAutomatico, 
									 String senhaUsuarioAutomatico, String paramAcaoAprovaRejeita, String urlBPM, boolean transferirRobo ) throws Exception{
		
		logger.info("INICIO aprovarEtapas");
		
		logger.info("atividadesExecutar : " + atividadesExecutar);
		
		if( !"".equals(atividadesExecutar) ){
		
			for (String atividadeExecut : atividadesExecutar.split(";")) {
				
				String[] paramFormEtapa = atividadeExecut.split("@");
				
				Integer codFormAnalise = new Integer(paramFormEtapa[0]);
				Integer codEtapaAnalise = new Integer(paramFormEtapa[1]);
				logger.info("PARAMETROS - Em analise - Form : " + codFormAnalise + " / Etapa : " + codEtapaAnalise);
	
				StringBuilder sqlConsultaEtapas = new StringBuilder();
				sqlConsultaEtapas.append(" 	   SELECT P.COD_FORM, PE.COD_PROCESSO, PE.COD_ETAPA, PE.COD_CICLO, P.IDE_BETA_TESTE ");
				sqlConsultaEtapas.append(" 		 FROM PROCESSO_ETAPA PE ");
				sqlConsultaEtapas.append(" INNER JOIN PROCESSO P ON ( P.COD_PROCESSO = PE.COD_PROCESSO ) ");
				sqlConsultaEtapas.append(" 		WHERE PE.IDE_STATUS IN ('A') ");
				sqlConsultaEtapas.append(" 		  AND P.COD_FORM = ");
				sqlConsultaEtapas.append(codFormAnalise);
				sqlConsultaEtapas.append(" 		  AND PE.COD_ETAPA IN ( ");
				sqlConsultaEtapas.append(codEtapaAnalise);
				sqlConsultaEtapas.append(" ) ");
				
				if ( !transferirRobo ) {
					sqlConsultaEtapas.append(" 		  AND PE.COD_USUARIO_ETAPA IN ( ");
					sqlConsultaEtapas.append(codUsuarioAutomatico);
					sqlConsultaEtapas.append(" ) ");
				}
				
				sqlConsultaEtapas.append("   ORDER BY PE.COD_PROCESSO, PE.COD_ETAPA, PE.COD_CICLO ");
				
				try (PreparedStatement pstConsultaEtapas = cnLecom.prepareStatement(sqlConsultaEtapas.toString());
						 ResultSet rsConsultaEtapas = pstConsultaEtapas.executeQuery();) {
				
					while (rsConsultaEtapas.next()) {
						
						Integer codProcesso = rsConsultaEtapas.getInt("COD_PROCESSO");
						Integer codEtapa = rsConsultaEtapas.getInt("COD_ETAPA");
						Integer codCiclo = rsConsultaEtapas.getInt("COD_CICLO");
						String modoTeste = rsConsultaEtapas.getString("IDE_BETA_TESTE");
						
						logger.info("Proc / Etapa : ( " + codProcesso + " / " + codEtapa + " ) ");
						
						if ( transferirRobo ) {
				    		if ( !verificaUsuarioProcessoEtapaUsu( cnLecom, codProcesso, codEtapa, codCiclo, codUsuarioAutomatico ) ) {
				    			inserirUsuarioEtapa ( cnLecom, codProcesso, codEtapa, codCiclo, codUsuarioAutomatico );
				    		}
						}
						
						if ( formNovo ) {
							logger.info("APROVACOES - RETORNO FN : Proc / Etapa - ( " + codProcesso + " / " + codEtapa + " ) - " + executaAtividProcessoFormNovo(codProcesso.toString(), codEtapa.toString(), codCiclo.toString(), modoTeste, null, null, codUsuarioAutomatico, loginUsuarioAutomatico, senhaUsuarioAutomatico, paramAcaoAprovaRejeita, urlBPM));
						
						} else {
//							String valores = "OBSERVACAO|Processo aprovado automaticamente.";
							String valores = "";
							logger.info("APROVACOES - RETORNO : Proc / Etapa - ( " + codProcesso + " / " + codEtapa + " ) - " + executaEtapaProcesso(rsConsultaEtapas.getInt("COD_FORM"), codProcesso, codEtapa, codCiclo, valores, paramAcaoAprovaRejeita, loginUsuarioAutomatico, senhaUsuarioAutomatico));
						}
					}
				}
				
			}
		}
		
		logger.info("FIM aprovarEtapas");
	}
	
	
	/*
	 * Aprova/Rejeita etapas, cujo campo DATA ( nomeCampoAnalise ),for igual ou inferior a data atual
	 */
	private void executarAtividadesDataCampo( Connection cnLecom, boolean formNovo, Calendar datAtual, String execucaoDataCampo, Integer codUsuarioAutomatico, String loginUsuarioAutomatico, 
											  String senhaUsuarioAutomatico, String paramAcaoAprovaRejeita, String urlBPM, boolean transferirRobo ) throws Exception{
		
		logger.info("INICIO executaEtapasDataCampo");
		logger.info("execucaoDataCampo : " + execucaoDataCampo);
		
		if( !"".equals(execucaoDataCampo) ){
		
			for (String paramExec : execucaoDataCampo.split(";")) {
				
				String[] paramFormEtapa = paramExec.split("@");
				
				Integer codFormAnalise = new Integer(paramFormEtapa[0]);
				Integer codEtapaAnalise = new Integer(paramFormEtapa[1]);
				String nomeCampoAnalise = paramFormEtapa[2];
				String nomeTabelaModelo = paramFormEtapa[3];
				logger.info("APROVACOES DATA CAMPO - Em analise - Form : " + codFormAnalise + " / Etapa : " + codEtapaAnalise + " / Campos : " + nomeCampoAnalise);
				
//				Map<String, String> paramFormModelo = Funcoes.getParametrosIntegracao(configPath + String.format("modelo_%1$s.properties", codFormAnalise));
//				String tableName = paramFormModelo.get("table_name");
			
				// Pega todos os processos que esto na etapa "Aguarda_data_modific" 
				StringBuilder sqlConsultaEtapas = new StringBuilder();
				sqlConsultaEtapas.append(" 	   SELECT P.COD_FORM, PE.COD_PROCESSO, PE.COD_ETAPA, PE.COD_CICLO, P.IDE_BETA_TESTE, F.");
				sqlConsultaEtapas.append(nomeCampoAnalise);
				sqlConsultaEtapas.append("	 	 FROM PROCESSO_ETAPA PE ");
				sqlConsultaEtapas.append(" INNER JOIN PROCESSO P ON ( P.COD_PROCESSO = PE.COD_PROCESSO ) ");
				sqlConsultaEtapas.append(" INNER JOIN ");
				sqlConsultaEtapas.append(nomeTabelaModelo);
				sqlConsultaEtapas.append(" F ON ( PE.COD_PROCESSO = F.COD_PROCESSO_F AND PE.COD_ETAPA = F.COD_ETAPA_F AND PE.COD_CICLO = F.COD_CICLO_F ) ");
				sqlConsultaEtapas.append("		WHERE PE.IDE_STATUS = 'A' ");
				sqlConsultaEtapas.append("  	  AND P.COD_FORM = ");
				sqlConsultaEtapas.append(codFormAnalise);
				sqlConsultaEtapas.append("  	  AND PE.COD_ETAPA = ");
				sqlConsultaEtapas.append(codEtapaAnalise);
				
				if ( !transferirRobo ) {
					sqlConsultaEtapas.append(" 		  AND PE.COD_USUARIO_ETAPA IN ( ");
					sqlConsultaEtapas.append(codUsuarioAutomatico);
					sqlConsultaEtapas.append(" ) ");
				}
				
				sqlConsultaEtapas.append("  AND F.");
				sqlConsultaEtapas.append(nomeCampoAnalise);
				sqlConsultaEtapas.append(" IS NOT NULL ");
		
	//			logger.info("sqlConsultaEtapas : " + sqlConsultaEtapas);
		
				try (PreparedStatement pstConsultaEtapas = cnLecom.prepareStatement(sqlConsultaEtapas.toString());
					 ResultSet rsConsultaEtapas = pstConsultaEtapas.executeQuery();) {
					
					while (rsConsultaEtapas.next()) {
						
						Integer codProcesso = rsConsultaEtapas.getInt("COD_PROCESSO");
						Integer codEtapa = rsConsultaEtapas.getInt("COD_ETAPA");
						Integer codCiclo = rsConsultaEtapas.getInt("COD_CICLO");
						String modoTeste = rsConsultaEtapas.getString("IDE_BETA_TESTE");
						Calendar datReferencia = DateToCalendar(rsConsultaEtapas.getDate(nomeCampoAnalise));
						
	//					logger.info("codProcesso : " + codProcesso);
	//					logger.info("codEtapa : " + codEtapa);
	//					logger.info("datReferencia : " + datReferencia);
						
						// Se a data Referencia for igual ou inferior a data atual, ento executa
						if (datReferencia.compareTo(datAtual) <= 0) {
							
							if ( transferirRobo ) {
					    		if ( !verificaUsuarioProcessoEtapaUsu( cnLecom, codProcesso, codEtapa, codCiclo, codUsuarioAutomatico ) ) {
					    			inserirUsuarioEtapa ( cnLecom, codProcesso, codEtapa, codCiclo, codUsuarioAutomatico );
					    		}
							}
							
							if ( formNovo ) {
								logger.info("APROVACOES - RETORNO FN : Proc / Etapa - ( " + codProcesso + " / " + codEtapa + " ) - " + executaAtividProcessoFormNovo(codProcesso.toString(), codEtapa.toString(), codCiclo.toString(), modoTeste, null, null, codUsuarioAutomatico, loginUsuarioAutomatico, senhaUsuarioAutomatico, paramAcaoAprovaRejeita, urlBPM));
							
							} else {
								String valores = "";
								logger.info("APROVACOES EXECUTA ETAPAS DATA CAMPO - RETORNO : Proc / Etapa - ( " + codProcesso + " / " + codEtapa + " ) - " + executaEtapaProcesso(rsConsultaEtapas.getInt("COD_FORM"), codProcesso, codEtapa, codCiclo, valores, paramAcaoAprovaRejeita, loginUsuarioAutomatico, senhaUsuarioAutomatico));
							}
						}
					}
				}
			}
		}
		
		logger.info("FIM executaEtapasDataCampo");
	}
	
	
	/*
	 * Aprova/Rejeita atividades cujo "prazo mximo", definido para ela, tenha excedido
	 */
	private void executarAtividadesPrazoMaximoExcedido( Connection cnLecom, boolean formNovo, Calendar datAtual, String paramProcExecutar, Integer codUsuarioAutomatico, 
												   		String loginUsuarioAutomatico, String senhaUsuarioAutomatico, String paramAcaoAprovaRejeita, String urlBPM, boolean transferirRobo ) throws Exception{
		
		logger.info("INICIO aprovarEtapasPrazoMaximoExcedido");
		
		logger.info("aprovacoesEtapasPrazoMaximoExcedido : " + paramProcExecutar);
		
		if( !"".equals(paramProcExecutar) ){
		
			for (String paramProc : paramProcExecutar.split(";")) {
				
				String[] paramFormEtapa = paramProc.split("@");
				
				// <cod form>@<cod etapa>@<nome do campo onservaao>@<mensagem para registrar execuo automtica>;
				Integer codFormAnalise = new Integer(paramFormEtapa[0]);
				Integer codEtapaAnalise = new Integer(paramFormEtapa[1]);
				String nomeCampoObservacao = "";
				String mensagemExecAutomatica = "";
				
				if( paramFormEtapa.length > 2 ){
					nomeCampoObservacao = paramFormEtapa[2];
					mensagemExecAutomatica = paramFormEtapa[3];
				}
				
				logger.info("APROVA ETAPAS PRAZO MAXIMO EXCEDIDO - Em analise - Form : " + codFormAnalise + " / Etapa : " + codEtapaAnalise + " / Campo Observao : " + nomeCampoObservacao + " / Mensagem : " + mensagemExecAutomatica);
	
				StringBuilder sqlConsultaAtividades = new StringBuilder();
				sqlConsultaAtividades.append(" 	   SELECT P.COD_FORM, PE.COD_PROCESSO, PE.COD_ETAPA, PE.COD_CICLO, E.COD_TIPO_ETAPA, PE.DAT_LIMITE, P.IDE_BETA_TESTE ");
				sqlConsultaAtividades.append(" 		 FROM PROCESSO_ETAPA PE ");
				sqlConsultaAtividades.append(" INNER JOIN PROCESSO P ON ( P.COD_PROCESSO = PE.COD_PROCESSO ) ");
				sqlConsultaAtividades.append(" INNER JOIN ETAPA E ON ( E.COD_FORM = P.COD_FORM AND E.COD_VERSAO = P.COD_VERSAO AND E.COD_ETAPA = PE.COD_ETAPA ) ");
				sqlConsultaAtividades.append(" 		WHERE PE.IDE_STATUS IN ('A') ");
				sqlConsultaAtividades.append(" 		  AND PE.DAT_LIMITE IS NOT NULL ");
				sqlConsultaAtividades.append(" 		  AND P.COD_FORM = ");
				sqlConsultaAtividades.append(codFormAnalise);
				sqlConsultaAtividades.append(" 		  AND PE.COD_ETAPA IN ( ");
				sqlConsultaAtividades.append(codEtapaAnalise);
				sqlConsultaAtividades.append(" ) ");
				
				if ( !transferirRobo ) {
					sqlConsultaAtividades.append(" 		  AND PE.COD_USUARIO_ETAPA IN ( ");
					sqlConsultaAtividades.append(codUsuarioAutomatico);
					sqlConsultaAtividades.append(" ) ");
				}
				
				sqlConsultaAtividades.append("   ORDER BY PE.COD_PROCESSO, PE.COD_ETAPA, PE.COD_CICLO ");
				
				logger.info("sqlConsultaAtividades : " + sqlConsultaAtividades);
				
				try ( PreparedStatement pstConsultaEtapas = cnLecom.prepareStatement(sqlConsultaAtividades.toString());
					  ResultSet rsConsultaEtapas = pstConsultaEtapas.executeQuery(); ) {
					
					while (rsConsultaEtapas.next()) {
						
						Integer codProcesso = rsConsultaEtapas.getInt("COD_PROCESSO");
						Integer codEtapa = rsConsultaEtapas.getInt("COD_ETAPA");
						Integer codCiclo = rsConsultaEtapas.getInt("COD_CICLO");
						String modoTeste = rsConsultaEtapas.getString("IDE_BETA_TESTE");
						Calendar datReferencia = DateToCalendar(rsConsultaEtapas.getDate("DAT_LIMITE"));
						
						// Se o tipo da etapa for 1 = Inicial, a aa precisa ser de cancelamento
						Integer codTipoEtapa = rsConsultaEtapas.getInt("COD_TIPO_ETAPA");
						if ( "R".equalsIgnoreCase(paramAcaoAprovaRejeita) && codTipoEtapa.compareTo(new Integer(1)) == 0 ){
							paramAcaoAprovaRejeita = "C";
							logger.info("nova ao : " + paramAcaoAprovaRejeita);
						}
						
						logger.info("codProcesso : " + codProcesso);
						logger.info("codEtapa : " + codEtapa);
						logger.info("datReferencia : " + datReferencia);
						
						// Se a data Referencia for igual ou inferior a data atual, ento executa
						if (datReferencia.compareTo(datAtual) <= 0) {
							
							if ( transferirRobo ) {
					    		if ( !verificaUsuarioProcessoEtapaUsu( cnLecom, codProcesso, codEtapa, codCiclo, codUsuarioAutomatico ) ) {
					    			inserirUsuarioEtapa ( cnLecom, codProcesso, codEtapa, codCiclo, codUsuarioAutomatico );
					    		}
							}
							
							if( formNovo ) {
								
								Map<String,String> camposValores = new HashMap<String, String>();
								
								if( !"".equals(nomeCampoObservacao) ) {
									camposValores.put(nomeCampoObservacao, mensagemExecAutomatica);
								}
								logger.info("APROVACOES - RETORNO FN : Proc / Etapa - ( " + codProcesso + " / " + codEtapa + " ) - " + executaAtividProcessoFormNovo(codProcesso.toString(), codEtapa.toString(), codCiclo.toString(), modoTeste, camposValores, null, codUsuarioAutomatico, loginUsuarioAutomatico, senhaUsuarioAutomatico, paramAcaoAprovaRejeita, urlBPM));
							
							} else {
								
								String valores = "";
								
								if( !"".equals(nomeCampoObservacao) ) {
									valores = nomeCampoObservacao + "|" + mensagemExecAutomatica;
								}
								
								logger.info("APROVACOES EXECUTA ETAPAS DATA CAMPO - RETORNO : Proc / Etapa - ( " + codProcesso + " / " + codEtapa + " ) - " + executaEtapaProcesso(rsConsultaEtapas.getInt("COD_FORM"), codProcesso, codEtapa, codCiclo, valores, paramAcaoAprovaRejeita, loginUsuarioAutomatico, senhaUsuarioAutomatico));
							}
						}
					}
				}
				
			}
		}
		
		logger.info("FIM aprovarEtapasPrazoMaximoExcedido");
	}
	
	
	// Gerar Access Token SSO
	private void gerarAccessToken530SSO(Connection cnLecom, String loginRobo, String senhaRobo, String urlSSo) throws Exception, LoginAuthenticationException {
		
		try {
			DadosLogin loginUtil = new DadosLogin(loginRobo, senhaRobo, false);
			LoginAutenticacao loginAutenticacao = new LoginAutenticacao(urlSSo, loginUtil);
			accessToken530SSO = loginAutenticacao.getToken();
			
		} catch (Exception e) {
			logger.error("[ ERRO ] : ", e);
		}
	}
	
	
	private static Calendar DateToCalendar(Date date){
		logger.info("INICIO DateToCalendar");
		
		Calendar cal = Calendar.getInstance();
		cal.setTime(date);
		
		logger.info("FIM DateToCalendar");
		
		return cal;
	}
	
	
	private boolean verificaUsuarioProcessoEtapaUsu (Connection cnLecom, Integer codProcesso, Integer codEtapa, Integer codCiclo, Integer codUsuario ) throws Exception {

		StringBuilder sql = new StringBuilder();
		sql.append(" SELECT COUNT(*) AS total ");
		sql.append("   FROM processo_etapa_usu ");
		sql.append("  WHERE cod_processo      = ? ");
		sql.append("    AND cod_etapa         = ? ");
		sql.append("    AND cod_ciclo         = ? ");
		sql.append("    AND cod_usuario_etapa = ? ");

		try (PreparedStatement pst = cnLecom.prepareStatement(sql.toString())) {
			pst.setInt(1, codProcesso);
			pst.setInt(2, codEtapa);
			pst.setInt(3, codCiclo);
			pst.setInt(4, codUsuario);

			try (ResultSet rs = pst.executeQuery()) {
				if (rs.next()) {
					int total = rs.getInt("total");

					if (total > 0) {
						return true;
					} else {
						return false;
					}
				} else {
					return false;
				}
			}
		}
	}
	
	
	// Insere o Usurio Robo na atividade para poder executa-la.
	private void inserirUsuarioEtapa (Connection cnLecom, Integer codProcesso, Integer codEtapa, Integer codCiclo, Integer codUsuario ) throws Exception {

		logger.info("[=== INICIO inserirUsuarioEtapa ===]");

		StringBuilder processoEtapaUsu = new StringBuilder();
		processoEtapaUsu.append(" INSERT INTO PROCESSO_ETAPA_USU ( COD_PROCESSO, COD_ETAPA, COD_CICLO, COD_USUARIO_ETAPA) ");
		processoEtapaUsu.append(" VALUES ( ?, ?, ?, ?) ");

		try(PreparedStatement pstInsert = cnLecom.prepareStatement(processoEtapaUsu.toString())) {

			pstInsert.setInt(1, codProcesso);
			pstInsert.setInt(2, codEtapa);
			pstInsert.setInt(3, codCiclo);
			pstInsert.setInt(4, codUsuario);

			pstInsert.executeUpdate();
			//cnLecom.commit();

		} catch (Exception e) {
			logger.error("[ERRO - inserirRoboEtapa]", e);
		}

		logger.info("[=== FIM inserirUsuarioEtapa ===]");
	}
	
	
	private void cancelaProcessoNaoEnviado( Connection cnLecom, LocalDate dataAtual, String atividadesExecutar, Integer codUsuarioAutomatico, String urlBPM, boolean transferirRobo ) throws Exception {

		logger.info("INICIO cancelaProcessoNaoEnviado");
		
		logger.info("atividadesExecutar : " + atividadesExecutar);
		
		if( !"".equals(atividadesExecutar) ){
			
			for ( String atividadeExecut : atividadesExecutar.split(";") ) {
				
				String[] paramFormEtapa = atividadeExecut.split("@");
				
				Integer codFormAnalise = new Integer(paramFormEtapa[0]);
				Integer auxQtdDiasAguardar = new Integer(paramFormEtapa[1]);
				int qtdDiasAguardar = -auxQtdDiasAguardar; // Deixa o numero negativo
				
				logger.info("PARAMETROS - Em analise - Form : " + codFormAnalise + " / qtdDiasAguardar : " + qtdDiasAguardar);
	
				StringBuilder sqlConsultaEtapas = new StringBuilder();
				sqlConsultaEtapas.append(" 	   SELECT P.COD_FORM, PE.COD_PROCESSO, PE.COD_ETAPA, PE.COD_CICLO, P.IDE_BETA_TESTE, PE.DAT_GRAVACAO ");
				sqlConsultaEtapas.append(" 		 FROM PROCESSO_ETAPA PE ");
				sqlConsultaEtapas.append(" INNER JOIN PROCESSO P ON ( P.COD_PROCESSO = PE.COD_PROCESSO ) ");
				sqlConsultaEtapas.append(" INNER JOIN ETAPA E ON ( E.COD_FORM = P.COD_FORM AND E.COD_VERSAO = P.COD_VERSAO AND E.COD_ETAPA = PE.COD_ETAPA ) ");
				sqlConsultaEtapas.append(" 		WHERE PE.IDE_STATUS = 'A' ");
				sqlConsultaEtapas.append(" 		  AND PE.IDE_TEMPORARIO = 'N' ");
				sqlConsultaEtapas.append(" 		  AND E.COD_TIPO_ETAPA = 1 ");
				sqlConsultaEtapas.append(" 		  AND PE.COD_CICLO = 1 ");
				sqlConsultaEtapas.append(" 		  AND P.COD_FORM = ");
				sqlConsultaEtapas.append(codFormAnalise);
				sqlConsultaEtapas.append("   ORDER BY PE.COD_PROCESSO, PE.COD_ETAPA, PE.COD_CICLO ");
				
				try ( PreparedStatement pstConsultaEtapas = cnLecom.prepareStatement(sqlConsultaEtapas.toString() );
					  ResultSet rsConsultaEtapas = pstConsultaEtapas.executeQuery(); ) {
				
					while (rsConsultaEtapas.next()) {
						
						Integer codProcesso = rsConsultaEtapas.getInt("COD_PROCESSO");
						Integer codEtapa = rsConsultaEtapas.getInt("COD_ETAPA");
						Integer codCiclo = rsConsultaEtapas.getInt("COD_CICLO");
						String modoTeste = rsConsultaEtapas.getString("IDE_BETA_TESTE");
//						Calendar dataAbertura = DateToCalendar(rsConsultaEtapas.getDate("DAT_GRAVACAO"));
						LocalDateTime dataAbertura = LocalDateTime.parse( rsConsultaEtapas.getString("DAT_GRAVACAO") , DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.S"));
						logger.info("dataAbertura : " + dataAbertura);
						
				        // Calcula a diferena de dias entre as duas datas ( Se a qtd vier negativa, quer dizer q a data Analisada  inferior a atual, se for positiva, a data analisada  superior a atual )
				        long difEmDiasDataAbertura = ChronoUnit.DAYS.between(dataAtual, dataAbertura);
				        logger.info("codProcesso / difEmDiasDataAbertura : " + codProcesso + " / " + difEmDiasDataAbertura);
						
						// Se a data Referencia for igual ou inferior a data atual, ento executa
					    if( qtdDiasAguardar > difEmDiasDataAbertura ) {
							
					    	if ( transferirRobo ) {
					    		if ( !verificaUsuarioProcessoEtapaUsu( cnLecom, codProcesso, codEtapa, codCiclo, codUsuarioAutomatico ) ) {
					    			inserirUsuarioEtapa ( cnLecom, codProcesso, codEtapa, codCiclo, codUsuarioAutomatico );
					    		}
							}
					    	
					    	logger.info("CANCELA PROCESSO - RETORNO FN : Proc / Etapa - ( " + codProcesso + " / " + codEtapa + " ) - " + cancelaProcessoFormNovo(codProcesso.toString(), codEtapa.toString(), codCiclo.toString(), modoTeste, codUsuarioAutomatico, urlBPM));
					    
					    } else {
					    	logger.debug("Aguardar!");
					    }
					    
					}
				}
				
			}
		}
		
		logger.info("FIM cancelaProcessoNaoEnviado");
	}
	
	
	@Override
	public List<WebServicesVO> getWebServices() {
		return null;
	}

	@Override
	public void setWebServices(WebServicesVO arg0) {
	}
}
