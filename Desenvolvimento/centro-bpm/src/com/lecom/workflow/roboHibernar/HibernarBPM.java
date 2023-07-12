package com.lecom.workflow.roboHibernar;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.lecom.tecnologia.db.DBUtils;

import br.com.lecom.atos.servicos.annotation.Execution;
import br.com.lecom.atos.servicos.annotation.RobotModule;
import br.com.lecom.atos.servicos.annotation.Version;

/**
 * @author matheus.scarpim
 *
 */
@RobotModule("HibernarBPM")
@Version({ 1,0,0})

public class HibernarBPM {

	private static final Logger logger = LoggerFactory.getLogger(HibernarBPM.class);
    private static final int DIA_AVISO = 5;
    private static final int TEMPO_HIBERNACAO_DEFAULT = 36000000;
    private static final int ETAPA = 1;
    


    @Execution
    public void executar() {
        Instant start = Instant.now();
        logger.info(new String(new char[150]).replace("\0", "#") + "\n");
        logger.info("[==== INICIO " + getClass().getSimpleName() + " ====]\n");

        try {

            try (Connection conn = DBUtils.getConnection()) {
                List<Map<String, String>> listProcessoPendente = coletaProcessosPendentesDeAlteracao(conn);
                if (listProcessoPendente.isEmpty()) {
                    logger.info("NAO HA PROCESSOS SEM DATA DE HIBERNACAO, NO MOMENTO.");
                }else{

                    logger.info("ALTERANDO RETORNO SLA...");
                    int dtDisparoIncluido = 0;
                    for (Map<String, String> p : listProcessoPendente) {    
                    	 String milliDTGravacao = p.get("DT_SOLICITACAO").toString();
                         logger.debug("milliDTGravacao: " + milliDTGravacao);
                         String diaVencimento = p.get("DT_EVENTO").toString();
                        logger.debug("diaVencimento: " + diaVencimento);
                        String prazo =  p.get("LST_PRAZO").toString() ;  
                        Long DiasRestantes = diasRestantes(diaVencimento);;
                        logger.debug("DiasRestantes: " + DiasRestantes);
                        if(DiasRestantes > 0) {
                            dtDisparoIncluido  += alterarTempoHibernacao(conn, p, DiasRestantes);
                        }else {
                            logger.error("N�o foi alterar o tempo de hibernacao do processo: "+ p.get("COD_PROCESSO") + " Dados do processo:");
                            logger.error("Cod Etapa: " + p.get("COD_ETAPA") );
                            logger.error("Ciclo: " + p.get("COD_CICLO"));
                            logger.error("--------------------------------");
                        }
                        logger.info("Total de Sla Alterada " + dtDisparoIncluido);
                    }

                }

            }

        } catch (Exception e) {
            logger.error("[==== ERRO DURANTE A EXECUCAO ====]");
            logger.error(exceptionPrinter(e));
            e.printStackTrace();
        } finally {
            Instant finish = Instant.now();

            long timeElapsed = Duration.between(start, finish).toMillis();
            logger.info("Tempo execucao em millis: " + timeElapsed);
            logger.info("Tempo execucao em segundos: " + Duration.between(start, finish).getSeconds());
            logger.info("[==== FIM " + getClass().getSimpleName() + " ====]\n");
        }

    }

    private static Long calcTempoHibernacao(Long dtGravacaoAtividade ,Integer diaVencimento) {
        LocalDateTime now = LocalDateTime.now();

        while (!isValid(now.getYear(), now.getMonthValue(), diaVencimento, now.getHour(), now.getMinute())) {
            diaVencimento--;
        }

        LocalDateTime vencimento = LocalDateTime.of(now.getYear(), now.getMonthValue(), diaVencimento, now.getHour(), now.getMinute());

        if(vencimento.getMonth() == now.getMonth()) {
            vencimento = vencimento.plusMonths(1);
        }
        ZonedDateTime zdt = vencimento.minusDays(DIA_AVISO).atZone(ZoneId.of("America/Sao_Paulo"));
        return zdt.toInstant().toEpochMilli() - dtGravacaoAtividade;
    }


    private static boolean isValid(int year, int month, Integer day, int hour, int minute) {
        boolean valid = false;

        try {
            LocalDateTime.of(year,month , day, hour, minute).minusDays(DIA_AVISO);
            valid = true;
        } catch (Exception e) {
            valid = false;
        }
        return valid;
    }
    
    private static long diasRestantes(String d2)
    {
        long valor = 0;
        try{
            // constrói a primeira data
            DateFormat fm = new SimpleDateFormat(
              "yyyy-MM-dd HH:mm:ss");
            LocalDateTime now = LocalDateTime.now();
            Date data1 = (Date)fm.parse(now.getYear()+"-"+ now.getMonthValue() +"-" +now.getDayOfMonth()+ " " + now.getHour() + ":" + now.getMinute()+ ":" + now.getSecond());
       
            fm = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            Date data2 = (Date)fm.parse(d2 + " 00:00:01");
       
            // vamos obter a diferença em semanas, dias, horas,
            // minutos e segundos
            long mili = data2.getTime() - data1.getTime();
            valor = mili;
       
        }catch(ParseException e){
            e.printStackTrace();
          }
        
        return valor;
    }



    /**
     * Converte o result set em um map com o nome da coluna e o valor
     * 
     * @param conn
     * @param rs
     * @return
     * @throws SQLException
     */
    private Map<String, String> getRetornoSelect(ResultSet rs) throws SQLException {
        Map<String, String> retorno = new HashMap<>();

        ResultSetMetaData rsmd = rs.getMetaData();

        for (int i = 1; i <= rsmd.getColumnCount(); i++)
            retorno.put(rsmd.getColumnName(i), nulo(rs.getObject(i), "").trim());

        return retorno;
    }


    /**
     * INCLUI A DATA QUE O PROCESSO DEVE SER DISPARADO NA GRID PAI
     * @param conn
     * @param dtSla
     * @param processo
     * @param vlrAtraso 
     * @return
     * @throws SQLException
     */
    private int alterarTempoHibernacao(Connection conn, Map<String, String> processo, Long tempoHibernacao) throws SQLException {
        int ret = 0;
        StringBuilder query = new StringBuilder();
        query.append(" UPDATE PROCESSO_ETAPA  ");
        query.append(" SET VLR_TEMP_HIBERNA = ?  ");
        query.append(" WHERE COD_PROCESSO = ? ");
        query.append("  AND COD_ETAPA = ? ");
        query.append("  AND COD_CICLO = ? ");

        try (PreparedStatement pst = conn.prepareStatement(query.toString());) {
            int index = 1;

            pst.setLong(index++, tempoHibernacao);
            pst.setInt(index++, Integer.parseInt(processo.get("COD_PROCESSO")));
            pst.setInt(index++, Integer.parseInt(processo.get("COD_ETAPA")));
            pst.setInt(index++, Integer.parseInt(processo.get("COD_CICLO")));


            ret = pst.executeUpdate();
        }
        return ret;

    }


    /**
     * Recupera processo que a necessidade de alterar o tempo de hiberna��o
     * @param conn
     * @return
     * @throws SQLException
     */
    private List<Map<String, String>> coletaProcessosPendentesDeAlteracao(Connection conn)
            throws SQLException {
        List<Map<String, String>> retorno = new ArrayList<>();
        StringBuilder query = new StringBuilder();
        query.append(" SELECT ");
        query.append("     * ");
        query.append(" FROM PROCESSO_ETAPA pe ");
        query.append(" INNER JOIN TABELA f ");
        query.append(" ON pe.COD_PROCESSO = f.COD_PROCESSO_F ");
        query.append("     AND pe.COD_ETAPA = f.COD_ETAPA_F ");
        query.append("     AND pe.COD_CICLO = f.COD_CICLO_F ");
        query.append(" WHERE pe.IDE_STATUS = 'H' ");
        query.append("     AND pe.VLR_TEMP_HIBERNA = " + TEMPO_HIBERNACAO_DEFAULT); 
        query.append("     AND pe.COD_ETAPA =  " + ETAPA);
        query.append("  ;");

        try(PreparedStatement pst = conn.prepareStatement(query.toString())){
            try(ResultSet rs = pst.executeQuery()){
                while(rs.next()) {
                    Map<String, String> out = getRetornoSelect(rs);
                    retorno.add(out);
                }
            }catch (Exception e) {
                logger.error("Erro ",e);
            }
        }
        return retorno;
    }

    /**
     * Fun��o que retorna em uma string o stack trace da exce��o passada como parametro.
     * @param e
     * @return
     */
    private String exceptionPrinter(Exception e) {
        StringBuilder sb = new StringBuilder();
        sb.append(e.toString());
        StackTraceElement[] el = e.getStackTrace();
        for(int i = 0; i < el.length; i++) {
            sb.append("\n\t at " + el[i].toString());
        }
        return sb.toString();
    }
    
    /***
     * Funcao que retorna a String recebida como parametro caso o objeto seja nulo
     * @param objeto
     * @param retorno
     * @return {@link String}
     */
    private String nulo(Object objeto, String retorno){
        String aux = "";

        if (objeto == null) {
            return retorno;
        } else {
            aux = objeto.toString();
            if(aux.equalsIgnoreCase("null")||aux.equalsIgnoreCase("")){
                return retorno;
            } else {
                return aux;
            }
        }
    }

}
