package com.lecom.workflow.integracoes.salvaValores;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.Timestamp;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;

import com.lecom.tecnologia.db.DBUtils;
import com.lecom.workflow.cadastros.common.util.RetornaInformacoesBPM;
import com.lecom.workflow.vo.IntegracaoVO;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import br.com.lecom.atos.servicos.annotation.Execution;
import br.com.lecom.atos.servicos.annotation.IntegrationModule;
import br.com.lecom.atos.servicos.annotation.Version;

@IntegrationModule("GuardaValoresPatrocinio")
@Version({2,0,0})
public class GuardaValoresBPM {
    private static final Logger logger = LoggerFactory.getLogger(GuardaValoresBPM.class);
    @Execution
    public String GuardaValoresBPM(IntegracaoVO integracaoVO) {
        try
        {
            @SuppressWarnings("unchecked")
            Map<String,String> camposEtapa = integracaoVO.getMapCamposFormulario();
            String cpf_cnpj = camposEtapa.get("$LT_CPF_CNPJ");



            RetornaInformacoesBPM retornarDadosBaseBPM = new RetornaInformacoesBPM();
           AddDadosTabela(retornarDadosBaseBPM,cpf_cnpj);

            logger.debug("Dados Adicionados na Tabela Patrocinio");

            return "0| Dados Adicionados na Tabela Patrocinio";
            }catch (Exception e) {
            // TODO: handle exception
            logger.error("Erro geral",e);
            return "99|Erro geral, por favor, contate o administrado do sistema";
        }

    }

    public static Timestamp convertStringToTimestamp(String strDate) {
        try {
          DateFormat formatter = new SimpleDateFormat("dd/MM/yyyy");
           // you can change format of date
          Date date = (Date) formatter.parse(strDate);
          Timestamp timeStampDate = new Timestamp(date.getTime());

          return timeStampDate;
        } catch (ParseException e) {
          System.out.println("Exception :" + e);
          return null;
        }
      }

    private static void AddDadosTabela(RetornaInformacoesBPM retornaDadosBaseBPM,String cpf_cnpj) throws Exception {


        try (Connection con = DBUtils.getConnection("aux_act")) {
                    StringBuilder sql = new StringBuilder();
                    sql.append("INSERT INTO ");
                    sql.append("tabela ");
                    sql.append("VALUES");
                    sql.append("(?);");
                    logger.debug(sql.toString());


                try (PreparedStatement pst = con.prepareStatement(sql.toString())) {
                    pst.setString(1, cpf_cnpj);

                    pst.execute();
                }
            }
        }
    }