package com.lecom.workflow.robo.AlteraSlaBPM;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Timestamp;
import java.util.Calendar;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.lecom.tecnologia.db.DBUtils;
import com.lecom.workflow.cadastros.common.util.CalculaTempoAtraso;
import com.lecom.workflow.cadastros.common.util.CalculaTempoLimite;
import com.lecom.workflow.cadastros.common.util.RetornaInformacoesBPM;
import com.lecom.workflow.robo.face.WebServices;
import com.lecom.workflow.robo.face.WebServicesVO;

import br.com.lecom.atos.servicos.annotation.Execution;
import br.com.lecom.atos.servicos.annotation.RobotModule;
import br.com.lecom.atos.servicos.annotation.Version;

@RobotModule("AlteraSlaBPM")
@Version({ 1, 0, 2 })
public class RoboAlteraSlaBPM implements WebServices {

	private static final Logger logger = LoggerFactory.getLogger(RoboAlteraSlaBPM.class);

	@Execution
	public void executaAlteracaoSla() {

		logger.debug("[============== Inicio do Robo RoboAlteraSlaBPM ==============]");

		try {
			RetornaInformacoesBPM retornarDadosBaseBPM = new RetornaInformacoesBPM();
			alteraSlaAnaliseDoc(retornarDadosBaseBPM);
		} catch (Exception e) {
			// TODO: handle exception
			logger.error("[ERRO] ::::", e);
			e.printStackTrace();
		}

		logger.debug("[============== T�rmino do Robo RoboAlteraSlaBPM ==============]");
	}

	private void alteraSlaAnaliseDoc(RetornaInformacoesBPM retornaDadosBaseBPM) throws Exception {

		Calendar dataGravacaoEtapa = Calendar.getInstance();
		Calendar dataLimite = Calendar.getInstance();
		Calendar dataAlerta = Calendar.getInstance();
		Map<String, Map<String, String>> turnoSemanaMap = null;
		List<String> diasNaoTrabalhadosList = null;
		long atraso1 = 0;
		long alerta1 = 0;

		try (Connection con = DBUtils.getConnection("workflow")) {
			StringBuilder verificaSlaQuery = new StringBuilder();
			verificaSlaQuery.append(
					"SELECT DISTINCT pe.cod_processo, pe.cod_etapa, pe.cod_ciclo, pe.dat_gravacao, pe.dat_limite, pe.dat_finalizacao, pe.vlr_tempo_alerta, pe.vlr_atraso1, f.campo");
			verificaSlaQuery.append("	FROM PROCESSO_ETAPA PE");
			verificaSlaQuery.append("		INNER JOIN PROCESSO P ON ( P.COD_PROCESSO = PE.COD_PROCESSO )");
			verificaSlaQuery.append(
					"		INNER JOIN ETAPA E ON ( E.COD_FORM = P.COD_FORM AND E.COD_VERSAO = P.COD_VERSAO AND E.COD_ETAPA = PE.COD_ETAPA )");
			verificaSlaQuery.append("		INNER JOIN tabela F on ( P.COD_PROCESSO = F.COD_PROCESSO_F )");
			verificaSlaQuery.append("			WHERE PE.IDE_STATUS = 'A'");
			verificaSlaQuery.append("				AND P.COD_FORM = 16");
			verificaSlaQuery.append("				AND PE.COD_ETAPA IN (5, 3);");

			// recupera o turno semanal que est� cadastrado no BPM
			turnoSemanaMap = retornaDadosBaseBPM.getTurnoSemanaMapBanco(con);

			// recupera os dias nao trabalhados por ser feriado
			diasNaoTrabalhadosList = retornaDadosBaseBPM.getDiasNaoTrabalhadosList(con);

			logger.debug("Query de consulta a ser executada" + verificaSlaQuery.toString());
			try (PreparedStatement pst = con.prepareStatement(verificaSlaQuery.toString())) {

				try (ResultSet rs = pst.executeQuery()) {

					while (rs.next()) {

						logger.debug(":::::: RETORNO DE DADOS DO PROCESSO ::::::");
						String tipoChamado = rs.getString("rb_nivel_prioridade");
						String codProcesso = rs.getString("cod_processo");
						String codEtapa = rs.getString("cod_etapa");
						String codCiclo = rs.getString("cod_ciclo");
						Timestamp dataGravacaoEtapaAtual = rs.getTimestamp("dat_gravacao");

						if (tipoChamado.equals("op1")) {
							// Calcula o tempo de atraso da etapa
							dataGravacaoEtapa.setTimeInMillis(dataGravacaoEtapaAtual.getTime());
							dataLimite = new CalculaTempoLimite(dataGravacaoEtapa, 0l, turnoSemanaMap,
									diasNaoTrabalhadosList).getCalculaTempoAtrasoSegundos(Long.parseLong("24"), "H");
							atraso1 = new CalculaTempoAtraso(dataLimite, dataGravacaoEtapa, turnoSemanaMap,
									diasNaoTrabalhadosList).getTotalEmMilisegundos();

							// Calcula o tempo de alerta da etapa
							dataGravacaoEtapa.setTimeInMillis(dataGravacaoEtapaAtual.getTime());
							dataAlerta = new CalculaTempoLimite(dataGravacaoEtapa, 0l, turnoSemanaMap,
									diasNaoTrabalhadosList).getCalculaTempoAtrasoSegundos(Long.parseLong("12"), "H");
							alerta1 = new CalculaTempoAtraso(dataAlerta, dataGravacaoEtapa, turnoSemanaMap,
									diasNaoTrabalhadosList).getTotalEmMilisegundos();
						} else if (tipoChamado.equals("op2")) {
							// Calcula o tempo de atraso da etapa
							dataGravacaoEtapa.setTimeInMillis(dataGravacaoEtapaAtual.getTime());
							dataLimite = new CalculaTempoLimite(dataGravacaoEtapa, 0l, turnoSemanaMap,
									diasNaoTrabalhadosList).getCalculaTempoAtrasoSegundos(Long.parseLong("6"), "H");
							atraso1 = new CalculaTempoAtraso(dataLimite, dataGravacaoEtapa, turnoSemanaMap,
									diasNaoTrabalhadosList).getTotalEmMilisegundos();

							// Calcula o tempo de alerta da etapa
							dataGravacaoEtapa.setTimeInMillis(dataGravacaoEtapaAtual.getTime());
							dataAlerta = new CalculaTempoLimite(dataGravacaoEtapa, 0l, turnoSemanaMap,
									diasNaoTrabalhadosList).getCalculaTempoAtrasoSegundos(Long.parseLong("4"), "H");
							alerta1 = new CalculaTempoAtraso(dataAlerta, dataGravacaoEtapa, turnoSemanaMap,
									diasNaoTrabalhadosList).getTotalEmMilisegundos();
						} else {
							// Calcula o tempo de atraso da etapa
							dataGravacaoEtapa.setTimeInMillis(dataGravacaoEtapaAtual.getTime());
							dataLimite = new CalculaTempoLimite(dataGravacaoEtapa, 0l, turnoSemanaMap,
									diasNaoTrabalhadosList).getCalculaTempoAtrasoSegundos(Long.parseLong("4"), "H");
							atraso1 = new CalculaTempoAtraso(dataLimite, dataGravacaoEtapa, turnoSemanaMap,
									diasNaoTrabalhadosList).getTotalEmMilisegundos();

							// Calcula o tempo de alerta da etapa
							dataGravacaoEtapa.setTimeInMillis(dataGravacaoEtapaAtual.getTime());
							dataAlerta = new CalculaTempoLimite(dataGravacaoEtapa, 0l, turnoSemanaMap,
									diasNaoTrabalhadosList).getCalculaTempoAtrasoSegundos(Long.parseLong("2"), "H");
							alerta1 = new CalculaTempoAtraso(dataAlerta, dataGravacaoEtapa, turnoSemanaMap,
									diasNaoTrabalhadosList).getTotalEmMilisegundos();
						}

						logger.debug("codProcesso = " + codProcesso);
						logger.debug("codEtapa = " + codEtapa);
						logger.debug("tipoPessoa = " + tipoChamado);
						logger.debug("codCiclo = " + codCiclo);
						logger.debug("dataGravacao apos tempoatraso = " + dataGravacaoEtapa.getTime());
						logger.debug("dataLimite apos tempoatraso = " + dataLimite.getTime());
						logger.debug("atraso1 = " + atraso1);
						logger.debug("datAlerta = " + dataAlerta.getTime());
						;
						logger.debug("alerta1 = " + alerta1);

						// Query de update para configurar o SLA da etapa
						StringBuilder sqlConfiguraSLA = new StringBuilder();
						sqlConfiguraSLA.append(" UPDATE ");
						sqlConfiguraSLA.append(" 	PROCESSO_ETAPA ");
						sqlConfiguraSLA.append(" SET ");
						sqlConfiguraSLA.append("     VLR_ATRASO1 = ? ");
						sqlConfiguraSLA.append("     , DAT_LIMITE = ? ");
						sqlConfiguraSLA.append("     , DAT_FINALIZACAO = ? ");
						sqlConfiguraSLA.append("	 , DAT_ALERTA = ?");
						sqlConfiguraSLA.append("     , VLR_TEMPO_ALERTA = ?");
						sqlConfiguraSLA.append(" WHERE ");
						sqlConfiguraSLA.append(" 	COD_PROCESSO = ? ");
						sqlConfiguraSLA.append(" 	AND COD_ETAPA = ? ");
						sqlConfiguraSLA.append(" 	AND COD_CICLO = ? ");

						try (PreparedStatement pstConfiguraSla = con.prepareStatement(sqlConfiguraSLA.toString())) {
							pstConfiguraSla.setLong(1, atraso1);
							pstConfiguraSla.setTimestamp(2, new Timestamp(dataLimite.getTimeInMillis()));
							pstConfiguraSla.setTimestamp(3, new Timestamp(dataLimite.getTimeInMillis()));
							pstConfiguraSla.setTimestamp(4, new Timestamp(dataAlerta.getTimeInMillis()));
							pstConfiguraSla.setLong(5, alerta1);
							pstConfiguraSla.setString(6, codProcesso);
							pstConfiguraSla.setString(7, codEtapa);
							pstConfiguraSla.setString(8, codCiclo);
							pstConfiguraSla.executeUpdate();
						}
					}
				}
			}
		}
	}

	@Override
	public List<WebServicesVO> getWebServices() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void setWebServices(WebServicesVO arg0) {
		// TODO Auto-generated method stub

	}
}