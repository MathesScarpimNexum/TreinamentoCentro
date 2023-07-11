package com.lecom.workflow.integracao.aberturaSubProcesso;


import java.io.File;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;

import com.lecom.tecnologia.db.DBUtils;
import com.lecom.workflow.cadastros.common.util.CalculaTempoAtraso;
import com.lecom.workflow.cadastros.common.util.CalculaTempoLimite;
import com.lecom.workflow.cadastros.common.util.Funcoes;
import com.lecom.workflow.cadastros.common.util.RetornaInformacoesBPM;
import com.lecom.workflow.cadastros.rotas.AbreProcesso;
import com.lecom.workflow.cadastros.rotas.LoginAutenticacao;
import com.lecom.workflow.cadastros.rotas.exception.AbreProcessoException;
import com.lecom.workflow.cadastros.rotas.exception.LoginAuthenticationException;
import com.lecom.workflow.cadastros.rotas.util.DadosLogin;
import com.lecom.workflow.cadastros.rotas.util.DadosProcesso;
import com.lecom.workflow.cadastros.rotas.util.DadosProcessoAbertura;
import com.lecom.workflow.vo.IntegracaoVO;

import br.com.lecom.atos.servicos.annotation.Execution;
import br.com.lecom.atos.servicos.annotation.IntegrationModule;
import br.com.lecom.atos.servicos.annotation.Version;

@IntegrationModule("CancelarSubProcessosPai")
@Version({1,0,2})
public class CancelarSubProcessosPai {


	private static final Logger logger = Logger.getLogger(CancelarSubProcessosPai.class);
	private static final String CAMINHOWF = Funcoes.getWFRootDir()+File.separator+"upload"+File.separator+"cadastros"+File.separator+"config"+File.separator;

	@Execution
	public String AbreProcesso(IntegracaoVO integracaoVO) {
		try
		{
			logger.debug("Iniciando Integracao");
			
			
			Map<String,String> camposEtapa = integracaoVO.getMapCamposFormulario();
			String lstProcesso = camposEtapa.get("$LT_NUMPROCESSO");
			String solic = camposEtapa.get("$RB_ACAO");
			logger.info(lstProcesso);
			logger.info(solic);
			if(solic.equals("Cancelar"))
			{
				buscarProcessos(lstProcesso);
			}
			
			logger.debug("Processos Fechados");
					
			return "0| Integração iniciada com sucesso";
		} catch (Exception e) {
			// TODO: handle exception
			logger.error("Erro geral",e);
			return "99|Erro geral, por favor, contate o administrado do sistema";
		}

	}
	
	public static void buscarProcessos(String codProcesso)
	{
		try(Connection conn = DBUtils.getConnection("workflow")) {
			Map<String,String> leituraNormativos = Funcoes.getParametrosIntegracao(CAMINHOWF + "DadosSubProcesso");
			StringBuilder busca = new StringBuilder();
			busca.append(" SELECT fl.COD_PROCESSO_F,p.COD_ETAPA_ATUAL,p.COD_CICLO_ATUAL  ");
			busca.append(" FROM processo p inner join ");
			busca.append(" tabela fl on (fl.COD_PROCESSO_F = p.COD_PROCESSO and fl.COD_ETAPA_F = p.COD_ETAPA_ATUAL and fl.COD_CICLO_F = p.COD_CICLO_ATUAL) ");
			busca.append(" and p.IDE_FINALIZADO = 'A' ");
			busca.append(" and p.COD_VERSAO = ? and fl.COD_PROCESSO_PAI = ? ");
			try (PreparedStatement pst = conn.prepareStatement(busca.toString())) {
				pst.setString(1, leituraNormativos.get("cod_versao"));
				pst.setString(2, codProcesso);

				try (ResultSet rs = pst.executeQuery()) {
					while (rs.next()) {
						logger.info(rs.getString("COD_PROCESSO_F"));
						logger.info(rs.getString("COD_ETAPA_ATUAL"));
						logger.info(rs.getString("COD_CICLO_ATUAL"));
						cancelaProcesso(conn, rs.getString("COD_PROCESSO_F"), rs.getString("COD_ETAPA_ATUAL"), rs.getString("COD_CICLO_ATUAL"));
					}
				}
			}
		} catch (Exception e) {
			logger.error("Erro:",e);
		}
	}
	
	public static void cancelaProcesso(Connection connBpm, String codProcesso, String codEtapa, String codCiclo) throws SQLException {
		StringBuilder updateProcesso = new StringBuilder();
		updateProcesso.append(" update processo set ide_finalizado = 'C' where cod_processo = ? ");
		try (PreparedStatement pstUpdateProcesso = connBpm.prepareStatement(updateProcesso.toString())) {
			pstUpdateProcesso.setString(1, codProcesso);

			pstUpdateProcesso.executeUpdate();
		}

		StringBuilder updateProcessoEtapa = new StringBuilder();
		updateProcessoEtapa.append(
				" update processo_etapa set ide_status = 'C' where cod_processo = ? and cod_etapa = ? and cod_ciclo = ? ");
		try (PreparedStatement pstUpdateProcessoEtapa = connBpm.prepareStatement(updateProcessoEtapa.toString())) {
			pstUpdateProcessoEtapa.setString(1, codProcesso);
			pstUpdateProcessoEtapa.setString(2, codEtapa);
			pstUpdateProcessoEtapa.setString(3, codCiclo);

			pstUpdateProcessoEtapa.executeUpdate();
		}

		StringBuilder insertProcessEvent = new StringBuilder();
		insertProcessEvent.append(
				" insert into process_instance_events (DAT_EVENT,MESSAGE,EVENT,PROCESS_ID,USER_ID) values (now(),?,0,?,1) ");
		try (PreparedStatement pstInsertEvent = connBpm.prepareStatement(insertProcessEvent.toString())) {
			pstInsertEvent.setString(1, "Processo cancelado pois o pai foi cancelado");
			pstInsertEvent.setString(2, codProcesso);

			pstInsertEvent.execute();
		}

	}
}
