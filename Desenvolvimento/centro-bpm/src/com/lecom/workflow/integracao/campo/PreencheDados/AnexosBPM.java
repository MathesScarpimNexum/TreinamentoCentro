package com.lecom.workflow.integracao.campo.PreencheDados;


import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.lecom.workflow.vo.IntegracaoVO;

import br.com.lecom.atos.servicos.annotation.Execution;
import br.com.lecom.atos.servicos.annotation.IntegrationModule;
import br.com.lecom.atos.servicos.annotation.Version;

@IntegrationModule("AnexosBPM")
@Version({1,0,1})
public class AnexosBPM {
	
	private static final Logger logger = LoggerFactory.getLogger(AnexosBPM.class);
	 
	@Execution
	public String retornaValoresAnexos(IntegracaoVO integracaoVO) {
		
		
//		
		
		String nome = "ANX_TESTE";
		String nomeAnexo= "Teste.docx"; 
		String nomeCriptografado = "9c21489a-a5b9-4b9a-be69-111371210019";
		String codCampo = "13";
		String codIdDocumento = "29";
		
		
		
		
		try {
			
			if (!nomeCriptografado.equals("")) {
				relacionarProcessoTemplate(integracaoVO, integracaoVO.getCodProcesso(),
						integracaoVO.getCodEtapa(), integracaoVO.getCodCiclo(), nomeCriptografado,
						Long.parseLong(codIdDocumento), codCampo);
				
				alteraValorCampoSaida(integracaoVO, nomeAnexo + ":" + nomeCriptografado,nome);
				
			}
			
			
			
			
			
			return "0|"+nomeAnexo + ":" + nomeCriptografado;
			
			
		} catch (NumberFormatException e) {
			logger.error("Erro ao parse em campo texto para numero ",e);
		} catch (Exception e) {
			logger.error("Erro geral ao relacionar arquivos ao processo ",e);
		}
		
		return "99|Erro ao carregar dados de arquivos padrões";
		
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
	
	
	public void alteraValorCampoSaida(IntegracaoVO integracaoVO, String valorCampoPDF, String nomeCampo) throws Exception {
		integracaoVO.setConexao("WorkFlow");
		try (Connection con = integracaoVO.getConexao()) {
			String update = "update " + integracaoVO.getNomeTabelaModelo()
					+ " set "+nomeCampo+" = ? where cod_processo_f = ? and cod_etapa_f = ? and cod_ciclo_f = ? ";
			try (PreparedStatement pst = con.prepareStatement(update)) {
				pst.setString(1, valorCampoPDF);
				pst.setString(2, integracaoVO.getCodProcesso());
				pst.setString(3, integracaoVO.getCodEtapa());
				pst.setString(4, integracaoVO.getCodCiclo());
				pst.executeUpdate();
			}

		}
	}
}
