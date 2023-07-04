package com.lecom.workflow.cadastros.common.util;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class RetornaInformacoesBPM {
	
	public RetornaInformacoesBPM() {
		// TODO Auto-generated constructor stub
	}
	
	public Map<String, Map<String, String>> getTurnoSemanaMapBanco(Connection connection) throws Exception {

		Map<String, Map<String, String>> turnoSemanaMap = new HashMap<String, Map<String,String>>();

		String sql = " select * from TURNO ";

		try (PreparedStatement pst = connection.prepareStatement(sql);) {

			try (ResultSet rs = pst.executeQuery();) {

				while (rs.next()) {
					Map<String, String> turnoDia = new HashMap<String, String>();

					// calcula e transforma registro em "hora:minuto"
					int inicio = rs.getInt("HORA_ENTRADA");
					int fim = rs.getInt("HORA_SAIDA");

					
					turnoDia.put("HORA_ENTRADA",String.valueOf(inicio));
					turnoDia.put("HORA_SAIDA", String.valueOf(fim));

					turnoSemanaMap.put(rs.getString("COD_TURNO"), turnoDia);
				}
			}
		} catch (Exception e) {
			throw e;
		}

		return turnoSemanaMap;
	}
	
	public List<String> getDiasNaoTrabalhadosList(Connection connection) throws Exception {

		SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy");

		List<String> diasNaoTrabalhadosList = new ArrayList<String>();

		String sql1 = " select * from FERIADO ";
		String sql2 = " select * from FERIADO_ANUAL ";

		try (PreparedStatement pst1 = connection.prepareStatement(sql1);
				ResultSet rs1 = pst1.executeQuery();) {

			while( rs1.next() ) {
				//diasNaoTrabalhadosList.add(dateFormat.format(rs1.getDate("DATA")));
				String dataFeriado = dateFormat.format(rs1.getDate("DATA"));
				String[] partesData = dataFeriado.split("/");
				diasNaoTrabalhadosList.add(partesData[2]+partesData[1]+partesData[0]);
			}

//			Calendar dataAtual = Calendar.getInstance();
//			String sAno = String.valueOf(dataAtual.get(Calendar.YEAR));

			try (PreparedStatement pst2 = connection.prepareStatement(sql2);
					ResultSet rs2 = pst2.executeQuery();) {

				while( rs2.next() ) {
					String sData = rs2.getString("DATA");
//					String sMes = sData.substring(0, 2);
//					String sDia = sData.substring(2);
					
					//diasNaoTrabalhadosList.add(sDia + "/" + sMes + "/" + sAno);
					diasNaoTrabalhadosList.add("FA"+sData);
				}
			}
		} catch (Exception e) {
			throw e;
		}

		return diasNaoTrabalhadosList;
	}
}
