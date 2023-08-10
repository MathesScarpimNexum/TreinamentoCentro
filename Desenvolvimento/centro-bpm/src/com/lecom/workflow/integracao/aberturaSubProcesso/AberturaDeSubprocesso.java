package com.lecom.workflow.integracao.aberturaSubProcesso;


import java.io.File;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.lecom.tecnologia.db.DBUtils;
import com.lecom.workflow.cadastros.common.util.Funcoes;
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

@IntegrationModule("AberturaDeSubprocesso")
@Version({1,0,0})
public class AberturaDeSubprocesso {


	private static final Logger logger = LoggerFactory.getLogger(AberturaDeSubprocesso.class);
	private static final String CAMINHOWF = Funcoes.getWFRootDir()+File.separator+"upload"+File.separator+"cadastros"+File.separator+"config"+File.separator;

	@Execution
	public String AbreProcesso(IntegracaoVO integracaoVO) {
		try
		{
			logger.debug("Iniciando Integracao");
			//loga ambiente
			Map<String,String> abertura = Funcoes.getParametrosIntegracao(CAMINHOWF + "automatico");
			Map<String,String> DadosSubProcesso = Funcoes.getParametrosIntegracao(CAMINHOWF + "DadosSubProcesso");
			String urlSSo = abertura.get("enderecoSSO");
			String loginUsuario = abertura.get("loginUsuarioAutomatico");
			String senhaUsuario  = abertura.get("senhaUsuarioAutomatico");
			boolean manterLogado = true;
			DadosLogin dadosLogin = new DadosLogin(loginUsuario,senhaUsuario, manterLogado);
	    	LoginAutenticacao loginAutentica = new LoginAutenticacao(urlSSo, dadosLogin);
	    	String token  = loginAutentica.getToken();
	    	logger.debug(token);
			
	    	//loga user

			String urlBPm = abertura.get("enderecoBPM");
			String codigoFormulario= DadosSubProcesso.get("cod_form");
			String codigoVersao = DadosSubProcesso.get("cod_versao");
			String modoTeste = DadosSubProcesso.get("modo_teste");
			
			Map<String,String> campos = new HashMap<>();
			@SuppressWarnings("unchecked")
			Map<String,String> camposEtapa = integracaoVO.getMapCamposFormulario();
			List<Map<String,Object>> valores = integracaoVO.getDadosModeloGrid("GD_ANEXOS");
			//String PA = camposEtapa.get("$LST_PA_SETOR");pegar valor de campo
			String nome = camposEtapa.get("$LT_NOME");
			String email = camposEtapa.get("$LT_EMAIL");
			
			
			List<Map<String,Object>> linhasGrid = new ArrayList<>();
			Map<String,Object> dadosGrid;
			for (Map<String, Object> map : valores) {
			    
			    dadosGrid = new HashMap<>();
				
				dadosGrid.put("ANX_EVIDENCIA",map.get("ANX_EVIDENCIA"));		
				
				linhasGrid.add(dadosGrid);	
			}
//			List<Map<String,Object>> valores2 = integracaoVO.getDadosModeloGrid("GD_ANEXO");
//			for (Map<String, Object> map : valores2) {
//			    
//			    dadosGrid = new HashMap<>();
//				
//				dadosGrid.put("ANX_ANEXO",map.get("ANX_ANEXO"));	
//				dadosGrid.put("LT_DESC_ANEXO",map.get("LT_DESC_ANEXO"));	
//				
//				linhasGrid.add(dadosGrid);
//			}// passar uma grid do processo pai para o filho
//			campos.put("LST_PA_SETOR",PA);
			logger.debug(" campos = "+campos);
			campos.put("LT_NOME",nome);
			campos.put("LT_EMAIL",email);
			campos.put("COD_PROCESSO_PAI", integracaoVO.getCodProcesso());
			
			
			DadosProcesso dadosProcesso = new DadosProcesso("A");
			dadosProcesso.geraValoresGrid("GD_ANEXOS", linhasGrid);	
			dadosProcesso.geraPadroes(campos);
			//add campos form
			
			for (Map<String, Object> map : valores) {
				AbreProcesso abreProcesso = new AbreProcesso(urlBPm, token, codigoFormulario,codigoVersao,modoTeste,abertura.get("codUsuarioAutomatico"),dadosProcesso);
				DadosProcessoAbertura dadosProcessoAbertura = abreProcesso.getAbreProcesso();//map.get("COD_USER").toString().replaceAll("\r\n", "")
				logger.debug(dadosProcessoAbertura.getProcessInstanceId());
				String CODProcess = dadosProcessoAbertura.getProcessInstanceId();
				String CODAtividade = dadosProcessoAbertura.getCurrentActivityInstanceId();
				String CODcycle = dadosProcessoAbertura.getCurrentCycle();
				inserirUsuarioEtapa(CODProcess, CODAtividade, CODcycle, map.get("COD_USER").toString().replaceAll("\r\n", ""),abertura);
			}
			
			logger.debug("Processos Abertos");
					
			return "0| processos abertos com sucesso";
		} catch (LoginAuthenticationException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();	
			logger.error("Erro ao logar",e);
			return "99|Falha a logar";
		} catch (AbreProcessoException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			logger.error("Erro ao abrir",e);
			return "99|Falha ao abrir";
		} catch (Exception e) {
			// TODO: handle exception
			logger.error("Erro geral",e);
			return "99|Erro geral, por favor, contate o administrado do sistema";
		}

	}
	
	
	private void inserirUsuarioEtapa(String codProcesso, String codEtapa, String codCiclo, String string,Map<String,String> automatico) throws SQLException {

        StringBuilder processoEtapaUsu = new StringBuilder();
        ExcluirUserAdm(codProcesso,codEtapa,codCiclo,automatico);
        processoEtapaUsu.append("INSERT INTO PROCESSO_ETAPA_USU ( COD_PROCESSO, COD_ETAPA, COD_CICLO, COD_USUARIO_ETAPA) ");
        processoEtapaUsu.append(" VALUES ( ?, ?, ?, ?) ");
        try(Connection connBpm = DBUtils.getConnection("workflow");
                PreparedStatement pst = connBpm.prepareStatement(processoEtapaUsu.toString())) {
            
            connBpm.setAutoCommit(false);
            
            pst.setString(1, codProcesso);
            pst.setString(2, codEtapa);
            pst.setString(3, codCiclo);
            pst.setString(4, string);

            pst.executeUpdate();
            connBpm.commit();
        } 
        ExcluirUserAdm(codProcesso,codEtapa,codCiclo,automatico);

    }

    private void ExcluirUserAdm(String codProcesso, String codEtapa, String codCiclo, Map<String,String> automatico) throws  SQLException{
        StringBuilder processoEtapaUsu = new StringBuilder();
        processoEtapaUsu.append(" DELETE FROM PROCESSO_ETAPA_USU  ");
        processoEtapaUsu.append(" where cod_usuario_etapa = ? and cod_processo = ? and cod_etapa = ?  and cod_ciclo = ? ");
        try(Connection connBpm = DBUtils.getConnection("workflow");
                PreparedStatement pst = connBpm.prepareStatement(processoEtapaUsu.toString())) {
            
            connBpm.setAutoCommit(false);
            
            pst.setString(1, automatico.get("codUsuarioAutomatico"));
            pst.setString(2, codProcesso);
            pst.setString(3, codEtapa);
            pst.setString(4, codCiclo);

            pst.executeUpdate();
            connBpm.commit();
        } 
    }




	public static String getWFRootDir() {
		String retorno = AberturaDeSubprocesso.class.getClassLoader().getResource("").getPath(); //classes
		retorno += "../"; //WEB-INF
		retorno += "../"; //workflow

		return retorno;
	}
}
