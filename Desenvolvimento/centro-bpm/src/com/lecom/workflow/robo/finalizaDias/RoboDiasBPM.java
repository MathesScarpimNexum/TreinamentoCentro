package com.lecom.workflow.robo.finalizaDias;

import java.io.File;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.lecom.tecnologia.db.DBUtils;
import com.lecom.workflow.cadastros.common.util.Funcoes;
import com.lecom.workflow.cadastros.rotas.AprovaProcesso;
import com.lecom.workflow.cadastros.rotas.LoginAutenticacao;
import com.lecom.workflow.cadastros.rotas.exception.AprovaProcessoException;
import com.lecom.workflow.cadastros.rotas.exception.LoginAuthenticationException;
import com.lecom.workflow.cadastros.rotas.util.DadosLogin;
import com.lecom.workflow.cadastros.rotas.util.DadosProcesso;
import com.lecom.workflow.cadastros.rotas.util.DadosProcessoAbertura;
import br.com.lecom.atos.servicos.annotation.Execution;
import br.com.lecom.atos.servicos.annotation.RobotModule;
import br.com.lecom.atos.servicos.annotation.Version;

// tip: each public class is put in its own file
@RobotModule("RoboDiasBPM")
@Version({ 1, 0, 1 })
public class RoboDiasBPM
{
    // tip: arguments are passed via the field below this editor
	private static final Logger logger = LoggerFactory.getLogger(RoboDiasBPM.class);
	  private static final String CAMINHOWF = Funcoes.getWFRootDir()+File.separator+"upload"+File.separator+"cadastros"+File.separator+"config"+File.separator;
	  private static final String CONEXAO_WORKFLOW = "workflow";
	  private static List<String> feriados = new ArrayList<String>();

	@Execution
    public void executar() {
        logger.info(new String(new char[80]).replace("\0", "#"));
        logger.info("[=== INICIO RoboDiasBPM ===]");

      try (Connection connBpm = DBUtils.getConnection(CONEXAO_WORKFLOW)) {
          consultaProcessoEmAndamento(connBpm);
      }catch (Exception e) {
          logger.error("Erro: ",e);// TODO: handle exception
        }

        logger.info("[=== FIM RoboDiasBPM ===]");
        logger.info(new String(new char[80]).replace("\0", "#") + "\n\n\n");
    }
	
    @SuppressWarnings("deprecation")
    public static void AnalisaData(String CodProcess,String CodEtapa,String Ciclo,String DataRetorno,String UltimoVoto, Connection con)
    {	
    	logger.info("AnalisaData");
    	Map<String,String> Chamado = null;
        try {
            Chamado = Funcoes.getParametrosIntegracao(CAMINHOWF + "RoboDiasBPM");
	    	Date datahoje = new Date();
	    	Date confereDataAbertura = null;
				confereDataAbertura = DataFormadata(DataRetorno);
			LocalDate diaHoje = converterLocalDate(confereDataAbertura);
			LocalDate SomadosDias = diaHoje.plusDays(Long.valueOf(Chamado.get("dias_finalizacao")));
	         diaHoje = getDiaUtil(SomadosDias);
	         confereDataAbertura = converterDate(diaHoje);
			if(confereDataAbertura.getTime() <= datahoje.getTime())
			{
				logger.info("Processo - " + CodProcess);
				processExecution(CodProcess, Ciclo, true);
			}
			else
			{
				logger.info("Nenhum processo pode ser avançado");
			}
        }catch (Exception e) {
			logger.error("Erro ",e);
		}
    		
    }
    
    public static Date DataFormadata(String DataRetorno)
    {
    	String data = DataRetorno;
    	SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss");
    	Date dataDate = null;
    	try {
			dataDate = sdf.parse(data);
		} catch (ParseException e) {
			// TODO Auto-generated catch block
			System.err.println(e);
			e.printStackTrace();
		}
		return dataDate;
    	
    }
    
    
    
    public void consultaProcessoEmAndamento(Connection connBpm) {
    	logger.info("consultaProcessoEmAndamento");
    	Map<String,String> Chamado = null;
        StringBuilder query = new StringBuilder();
        query.append("SELECT DISTINCT pe.DAT_GRAVACAO,p.COD_CICLO_ATUAL,RB_VOTACAO,p.COD_ETAPA_ATUAL,p.cod_processo FROM processo p INNER JOIN processo_etapa pe ON ( p.COD_PROCESSO = pe.COD_PROCESSO) inner join f_treinamento_j f on (f.COD_PROCESSO_F = pe.COD_PROCESSO) where  p.COD_FORM = ? and (pe.COD_ETAPA = ?) and IDE_STATUS = 'A' and COD_VERSAO = ? order by p.COD_CICLO_ATUAL desc;");
 
        try (PreparedStatement pst = connBpm.prepareStatement(query.toString())) {
        	 Chamado = Funcoes.getParametrosIntegracao(CAMINHOWF + "RoboDiasBPM");
        	pst.setString(1, Chamado.get("cod_form"));
        	pst.setString(2, Chamado.get("cod_etapa"));
        	pst.setString(3, Chamado.get("cod_versao"));
            ResultSet rs = pst.executeQuery();
            while (rs.next()) {
                String datGravacao = rs.getString("DAT_GRAVACAO");
                String CICLO = rs.getString("COD_CICLO_ATUAL");
                String UltimoVoto = rs.getString("RB_VOTACAO");
                String etapa = rs.getString("COD_ETAPA_ATUAL");
                String processo = rs.getString("cod_processo");
                if(UltimoVoto != null || UltimoVoto != "" || UltimoVoto != " ")
                {
                	AnalisaData(processo,etapa,CICLO,datGravacao,UltimoVoto,connBpm);
                }
            }
        } catch (SQLException e) {
            logger.error("Falha ao consultar processos em andamentos :: ", e);
        } catch (Exception e) {
			// TODO Auto-generated catch block
        	logger.error("Erro :", e);
			e.printStackTrace();
		}

    }
    
    public static void inserirUsuarioEtapa(String codProcesso, String codEtapa, String codCiclo, String codigoPessoas ) throws SQLException {

        StringBuilder processoEtapaUsu = new StringBuilder();
        processoEtapaUsu.append("INSERT INTO PROCESSO_ETAPA_USU ( COD_PROCESSO, COD_ETAPA, COD_CICLO, COD_USUARIO_ETAPA) ");
        processoEtapaUsu.append(" VALUES ( ?, ?, ?, ?) ");
        try(Connection connBpm = DBUtils.getConnection("workflow");
                PreparedStatement pst = connBpm.prepareStatement(processoEtapaUsu.toString())) {
            
            connBpm.setAutoCommit(false);
            
            pst.setString(1, codProcesso);
            pst.setString(2, codEtapa);
            pst.setString(3, codCiclo);
            pst.setString(4, codigoPessoas);

            pst.executeUpdate();
            connBpm.commit();
        }catch (Exception e) {
			logger.error("Erro : ", e);
		}

    }
	
	private static String processExecution(String codProcesso, String codCiclo,
            boolean aprovarProcesso) {
		
        String ret = "";
        Map<String,String> abertura = null;
         Map<String,String> Chamado = null;
        try {
            Chamado = Funcoes.getParametrosIntegracao(CAMINHOWF + "RoboDiasBPM");
            abertura = Funcoes.getParametrosIntegracao(CAMINHOWF + "automatico");
        } catch (Exception e1) {
            // TODO Auto-generated catch block
            e1.printStackTrace();
        }
        
        String Atividade = Chamado.get("cod_etapa");
        
        try {
			inserirUsuarioEtapa(codProcesso, Atividade, codCiclo, abertura.get("codUsuarioAutomatico"));
		} catch (SQLException e1) {
			// TODO Auto-generated catch block
			logger.error("Erro ao inserir ",e1);
			e1.printStackTrace();
		}

        String sso = abertura.get("enderecoSSO");
        String bpm = abertura.get("enderecoBPM");
        String test = Chamado.get("modo_teste");

        String user = abertura.get("codUsuarioAutomatico");
        String login = abertura.get("loginUsuarioAutomatico");
        String pw = abertura.get("senhaUsuarioAutomatico");
        

        String token = applicationLogin(sso, login, pw);
        logger.debug(sso);
        logger.debug(bpm);
        logger.debug(test);
        logger.debug(user);
        logger.debug(login);
        logger.debug(pw);
        logger.debug(token);
        
        logger.debug(Atividade);
        
        logger.debug(codProcesso);
        logger.debug(codCiclo);
        logger.debug(token);
        String aprovacaoRejeicao = aprovarProcesso ? "P" : "R";

        try {
            DadosProcesso dadosProcesso = new DadosProcesso(aprovacaoRejeicao);

            DadosProcessoAbertura dadosProcessoAbertura = new DadosProcessoAbertura();
            dadosProcessoAbertura.setProcessInstanceId(codProcesso.toString());
            dadosProcessoAbertura.setCurrentActivityInstanceId(Atividade);
            dadosProcessoAbertura.setCurrentCycle(codCiclo.toString());
            dadosProcessoAbertura.setModoTeste(test);
            AprovaProcesso aprovaProcesso = new AprovaProcesso(bpm, token, dadosProcessoAbertura, dadosProcesso, test,
                    user);
            ret = aprovaProcesso.aprovaProcesso();
        } catch (AprovaProcessoException e) {
            logger.error("AprovaProcessoException: ", e);
            e.printStackTrace();
        }

        return ret;
    }

    /**
     * Realiza login no BPM.
     *
     * @param URL   do ambiente
     * @param Login do usuÃ¡rio automÃ¡tico
     * @param Senha do usuÃ¡rio automÃ¡tico
     * @return Token do login
     */
    private static String applicationLogin(String url, String login, String pw) {

        String ret = "";

        try {
            DadosLogin loginUtil = new DadosLogin(login, pw, true);
            LoginAutenticacao loginAuteAuthentication = new LoginAutenticacao(url, loginUtil);
            ret = loginAuteAuthentication.getToken();
        } catch (LoginAuthenticationException e) {
            logger.error("LoginAuthenticationException - Method applicationLogin: ", e);
            e.printStackTrace();
        }
        return ret;
    }
    
    
    public static LocalDate getDiaUtil(LocalDate dataPrazo) {
		Calendar calendar = Calendar.getInstance();
	    calendar.clear();
	    //assuming start of day
	    calendar.set(dataPrazo.getYear(), dataPrazo.getMonthValue()-1, dataPrazo.getDayOfMonth());
	      
		SimpleDateFormat formataAno = new SimpleDateFormat("yy"); 
		SimpleDateFormat formataDiaMes = new SimpleDateFormat("MMdd");
		
		if( !dataPrazo.getDayOfWeek().equals(DayOfWeek.SATURDAY) 
			&& !dataPrazo.getDayOfWeek().equals(DayOfWeek.SUNDAY)
			&& !isFeriado(formataDiaMes.format(calendar.getTime()), formataAno.format(calendar.getTime()))
				) {
			System.out.println(" N�o � Feriado" + dataPrazo);
			return dataPrazo;
			
		} else {
			System.out.println("Feriado" + dataPrazo);
			return getDiaUtil(dataPrazo.plusDays(1));
		}
	}
	
	private static boolean isFeriado(String diaMes, String ano) {
			
			// FA = Feriado Anual
			if(feriados != null && feriados.size() > 0){
				
				// Testa se eh um Feriado Anual e/ou um Feriado
				// FA1225 - Natal (Feriado Anual)
				return (feriados.contains("FA" + diaMes) ? true : feriados.contains(ano+diaMes));
				
			}
			
			return false;
		}	
	
	public List<String>  getDiasNaoTrabalhadosList() throws Exception {

		SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy");

		List<String> diasNaoTrabalhadosList = new ArrayList<String>();

		String sql1 = " select * from FERIADO ";
		String sql2 = " select * from FERIADO_ANUAL ";
		try(Connection connection = DBUtils.getConnection("workflow")){
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
		} catch (Exception e) {
			throw e;
		}
		
		return diasNaoTrabalhadosList;

	}
	
	public static Date converterDate(LocalDate data)
	{
		LocalDate localDate = data;
        System.out.println("LocalDate = " + localDate);

        Date date1 = Date.from(localDate.atStartOfDay(ZoneId.systemDefault()).toInstant());
        System.out.println("Date      = " + date1);

		return date1;
		
	}
	
	public static LocalDate converterLocalDate(Date data)
	{
		LocalDate local;
		local = data.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
        System.out.println("LocalDate = " + local);
        return local;
	}
}
