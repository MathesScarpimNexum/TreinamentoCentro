 package com.lecom.workflow.cadastros.aplicacaoExt.controller;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Calendar;
import java.util.Date;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;
import org.apache.log4j.spi.LoggerFactory;
import org.json.JSONObject;

import com.lecom.tecnologia.db.DBUtils;
import com.lecom.workflow.cadastros.common.util.Funcoes;
import com.lecom.workflow.cadastros.rotas.AbreProcesso;
import com.lecom.workflow.cadastros.rotas.LoginAutenticacao;
import com.lecom.workflow.cadastros.rotas.exception.AbreProcessoException;
import com.lecom.workflow.cadastros.rotas.exception.LoginAuthenticationException;
import com.lecom.workflow.cadastros.rotas.util.DadosLogin;
import com.lecom.workflow.cadastros.rotas.util.DadosProcessoAbertura;

@WebServlet("/app/public/aplicacaoExt")
public class AplicacaoController extends HttpServlet {
    
    /**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private static final String CONEXAO_WORKFLOW = "workflow";
    private static final String CAMINHOWF = Funcoes.getWFRootDir()+File.separator+"upload"+File.separator+"cadastros"+File.separator+"config"+File.separator;
    private static final Logger logger = LoggerFactory.getLogger(AplicacaoController.class);
    
    @Override
	public void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		// TODO Auto-generated method stub
		
		logger.debug("Entrou");
	    
		String action = req.getParameter("action");
		if(action.equals("getProcessosAndamento")) {
		    try (Connection connBpm = DBUtils.getConnection(CONEXAO_WORKFLOW)) {
		        ConsultaProcessosAndamento(req, resp, connBpm);
	          }catch (Exception e) {
	              logger.error("Erro: ",e);// TODO: handle exception
	            }

		}
		if(action.equals("getResponder")) {
            try (Connection connBpm = DBUtils.getConnection(CONEXAO_WORKFLOW)) {
                getReponderSolic(req, resp);
              }catch (Exception e) {
                  logger.error("Erro: ",e);// TODO: handle exception
                }

        }
		
		if(action.equals("AbrirProcesso")) {
            try (Connection connBpm = DBUtils.getConnection(CONEXAO_WORKFLOW)) {
                AbrirNovo(req, resp);
              }catch (Exception e) {
                  logger.error("Erro: ",e);// TODO: handle exception
                }

        }
        
		
	}


	public void ConsultaProcessosAndamento(HttpServletRequest request, HttpServletResponse resp, Connection connBpm) throws ServletException, IOException 
	{
		// TODO Auto-generated method stub
	    
	   try {
	       JSONObject retorno = new JSONObject();
	       String cod_processo = request.getParameter("cod");
	       String Status = null;
	       String Finalizacao = null;
	       
	       if(cod_processo == "VAZIO")
	       {
	    	   retorno.put("retorno", "Error");
	    	   PrintWriter out = resp.getWriter();
		          resp.setContentType("application/json");
		          resp.setCharacterEncoding("UTF-8");
		          out.print(retorno.toString());
		          out.close();
	       }
	       else
	       {
	    	   Map<String,String> usuario_publico = Funcoes.getParametrosIntegracao(CAMINHOWF + "UsuarioPublico");

		          StringBuilder query = new StringBuilder();
		          query.append("SELECT distinct ");
		          query.append("pe.IDE_STATUS,p.IDE_FINALIZADO ");
		          query.append("FROM tabela f,processo p ");
		          query.append("inner join processo_etapa pe on ");
		          query.append("(p.COD_PROCESSO = pe.COD_PROCESSO) ");
		          query.append("where p.COD_FORM = ? and ");
		          query.append("p.COD_PROCESSO = ? ");
		          query.append("and pe.COD_ETAPA = '4' order ");
		          query.append("by pe.COD_CICLO desc limit 1;");
		          

		          try (PreparedStatement pst = connBpm.prepareStatement(query.toString())) {
		              pst.setString(1, usuario_publico.get("form"));
		              pst.setString(2, cod_processo.toString());
		              ResultSet rs = pst.executeQuery();
		              while (rs.next()) {
		                  Status = rs.getString("IDE_STATUS");
		                  Finalizacao = rs.getString("IDE_FINALIZADO");
		              }
		          } catch (SQLException e) {
		              logger.error("Falha ao consultar processos em andamentos :: ", e);
		          }
		          if(Status != null)
		          {
		        	  if(Status.equals("A") && !Finalizacao.equals("P"))
			          {
			              retorno.put("retorno", "Em Análise");
			          }
			          else if(Finalizacao.equals("P"))
			          {
			              retorno.put("retorno", "Finalizado");
			          }
			          else
			          {
			              retorno.put("retorno", "Analisado");
			          }
		          }
		          else
		          {
		        	  retorno.put("retorno", "Não Existe");
		          }
		          PrintWriter out = resp.getWriter();
		          resp.setContentType("application/json");
		          resp.setCharacterEncoding("UTF-8");
		          out.print(retorno.toString());
		          out.close();
	       }}catch (Exception e) {
			logger.error(e);
	       	}
	       
	   
	}
	
	public void getReponderSolic(HttpServletRequest request, HttpServletResponse resp) throws ServletException, IOException
	{
	    Map<String, String> usuario_publico = null;
        try {
            usuario_publico = Funcoes.getParametrosIntegracao(CAMINHOWF + "UsuarioPublicoEtica");
        } catch (Exception e1) {
            // TODO Auto-generated catch block
            e1.printStackTrace();
        }
	    String codProcesso = request.getParameter("cod");
        String codEtapa = "5";
        String CodCiclo = "";
        String uuid = "";
	    try(Connection con = DBUtils.getConnection("workflow")){
	    	StringBuilder query = new StringBuilder();
	          query.append("SELECT distinct pe.uuid,pe.cod_ciclo ");
	          query.append("FROM processo p ");
	          query.append("inner join processo_etapa pe ");
	          query.append("on (p.cod_processo = pe.COD_PROCESSO) ");
	          query.append("WHERE p.cod_processo = ? AND pe.cod_etapa = ? ");
	          query.append( "order ");
	          query.append("by pe.COD_CICLO desc limit 1");
            
            try(PreparedStatement pst = con.prepareStatement(query.toString())){
                pst.setString(1, codProcesso.toString());
                pst.setString(2, codEtapa);
                
                try(ResultSet rs = pst.executeQuery()){
                    if(rs.next()) { 
                        uuid = rs.getString("uuid");
                        CodCiclo = rs.getString("cod_ciclo");     
                        logger.info(uuid.toString());
                        logger.info(CodCiclo.toString());
                    }
                }
                
            }
        } catch (SQLException e) {
            logger.error("Erro ao realizar select ",e);
        }
	    StringBuilder url = new StringBuilder();
        url.append("https://" + usuario_publico.get("ambiente") +".cooperativa.lecom.com.br/form-web/process-instances/activity-forms?");
        url.append("processInstanceId="+codProcesso);
        url.append("&activityInstanceId="+codEtapa);
        url.append("&cycle="+CodCiclo);
        url.append("&uuid="+uuid);
        url.append("&language=pt_BR&displayFormHeader=false");
        
        resp.setContentType("application/json");
        PrintWriter out = resp.getWriter();
        out.print(url.toString());
        out.close();
        
        //request.setAttribute("url", url.toString());
        
        
        //request.getServletContext().getRequestDispatcher("/upload/cadastros/AplicacaoExternaEtica/pages/home.jsp").forward(request, resp);   
	}
    public void AbrirNovo(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
    	Date date = new Date(); // sua instancia Date
    	Calendar calendar = Calendar.getInstance();
    	calendar.setTime(date);
            logger.debug("Entrei");
            try {
                 Map<String,String> usuario_publico = Funcoes.getParametrosIntegracao(CAMINHOWF + "UsuarioPublicoEtica");
                DadosLogin dadosLogin = new DadosLogin(usuario_publico.get("login"),usuario_publico.get("senha"), false);
                LoginAutenticacao loginAuthentication = new LoginAutenticacao(usuario_publico.get("sso"), dadosLogin);
                String token = loginAuthentication.getToken();
                logger.info(token);
                AbreProcesso abreProcesso = new AbreProcesso(usuario_publico.get("bpm"), token, usuario_publico.get("form"), usuario_publico.get("versao"), "false",null, null);
                
                DadosProcessoAbertura dadosProcessoAbertura = abreProcesso.getAbreProcesso();
                String codProcesso = dadosProcessoAbertura.getProcessInstanceId();;
                String codEtapa = dadosProcessoAbertura.getCurrentActivityInstanceId();
                String codCiclo = dadosProcessoAbertura.getCurrentCycle();
                String uuid = "";
                
                StringBuilder url = new StringBuilder();
                url.append("https://" + usuario_publico.get("ambiente") +".cooperativa.lecom.com.br/form-web/process-instances/activity-forms?");
                url.append("processInstanceId="+codProcesso);
                url.append("&activityInstanceId="+codEtapa);
                url.append("&cycle="+codCiclo);
                
                logger.info(url.toString());
                
                
                try(Connection con = DBUtils.getConnection("workflow")){
                    String sql = "SELECT uuid FROM processo_etapa WHERE cod_processo = ? AND cod_etapa = ? and cod_ciclo = ?";
                    
                    try(PreparedStatement pst = con.prepareStatement(sql)){
                        pst.setString(1, codProcesso);
                        pst.setString(2, codEtapa);
                        pst.setString(3, codCiclo);
                        
                        try(ResultSet rs = pst.executeQuery()){
                            if(rs.next()) {
                                uuid = rs.getString("uuid");                        
                            }
                        }
                    }
                } catch (SQLException e) {
                    logger.error("Erro ao realizar select ",e);
                }
                url.append("&uuid="+uuid);
                url.append("&language=pt_BR&displayFormHeader=false");
                
                
                logger.info(url.toString());
                
                response.setContentType("application/json");
                PrintWriter out = response.getWriter();
                out.print(url.toString());
                out.close();
                //response.sendRedirect(url.toString());
            }catch (LoginAuthenticationException e) {
                logger.error("Erro ao realizar login",e);
            } catch (AbreProcessoException e1) {
                logger.error("Erro ao abrir processo",e1);
            } catch (Exception e) {
                logger.error("Erro geral",e);
            }
            //request.getServletContext().getRequestDispatcher("/upload/cadastros/AplicacaoExternaEtica/pages/home.jsp").forward(request, response);     
            
        }
}
