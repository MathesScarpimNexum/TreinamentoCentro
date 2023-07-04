package com.lecom.workflow.cadastros.common.util;

import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.List;
import java.util.Map;


public class CalculaTempoLimite {

	private Calendar dataGravacao = null;
	private Long tempoAtraso;
	private Map<String, Map<String, String>> turnos;
	private List<String> feriados;
	
	private SimpleDateFormat formataAno = new SimpleDateFormat("yy"); 
	private SimpleDateFormat formataDiaMes = new SimpleDateFormat("MMdd");
	private SimpleDateFormat formataHora = new SimpleDateFormat("HH:mm:ss");
	
    public CalculaTempoLimite(Calendar dataGravacao, Long tempoAtraso, Map<String, Map<String, String>> turnos, List<String> feriados) {
    	
		this.dataGravacao = dataGravacao;
		this.tempoAtraso = tempoAtraso;
		this.turnos = turnos;
		this.feriados = feriados;
		
	}

	public Calendar getDataLimite() {
		
		Calendar dataLimite = Calendar.getInstance();
		dataLimite.setTime(new Timestamp(dataGravacao.getTimeInMillis()));
		
        boolean bFeriado   = false;
        boolean bAux       = false;
        long iAtraso       = 0;
        long iHoraSobra    = 0; //variável auxiliar com a quantidade de horas que já foram "consumidas"

        long iHoraDia      = 0;
        long iHoraMax      = 0; //horas máximas de um dia

        long horaEntrada   = 0;
        long horaSaida     = 0;
        long iHoraFinal    = 0;

        String sHora       = "";

        // se não existir turno considerar que não existe atraso
        tempoAtraso = (turnos == null || turnos.size() == 0) ? 0 : tempoAtraso;


        horaEntrada   = 0;
        horaSaida     = 0;

        if (tempoAtraso != 0) {
        	//TODO ira trabalhar em segundos
            iAtraso = tempoAtraso; //ira trabalhar em segundos

            bFeriado = isFeriado(formataDiaMes.format(dataLimite.getTime()), formataAno.format(dataLimite.getTime()));

            if (bFeriado) {
                iHoraMax = 0;
            } else {
            	
				int diaDaSemana = dataLimite.get(Calendar.DAY_OF_WEEK);

				// Pega o turno de acordo com o dia da semana
				Map<String, String> turno = getTurno(diaDaSemana);

//				horaEntrada = turno.getHoraEntrada();
//				horaSaida = turno.getHoraSaida();         
				horaEntrada = Integer.parseInt(turno.get("HORA_ENTRADA"));
				horaSaida = Integer.parseInt(turno.get("HORA_SAIDA"));
            	

                 //TODO ira trabalhar em segundos
                horaEntrada *= 60;
                horaSaida *= 60;
                
                //pega a quantidade de horas que o dia da gravação ainda tem
               //TODO ira trabalhar em segundos
                sHora = formataHora.format(dataLimite.getTime());
                iHoraDia  = getSegundos(sHora);
                
                // Nao ira considerar das 00:01 até o inicio do turno (hora entrada)
                if(iHoraDia < horaEntrada)
                	iHoraDia = horaEntrada;                
                
                //calcula a qtde máxima de horas que usará naquele dia
                iHoraMax  = horaSaida - iHoraDia;
                if (iHoraMax < 0) iHoraMax = 0;
            }

            iHoraSobra = iAtraso - iHoraMax;
            
            if (iHoraSobra <= 0) {
                iHoraFinal = iAtraso;
                bAux = true;
            }

            while (iHoraSobra > 0) {
                // ve o dia que está
                dataLimite.add(Calendar.DATE, 1);

                bFeriado = isFeriado(formataDiaMes.format(dataLimite.getTime()), formataAno.format(dataLimite.getTime()));

                if (bFeriado) {
                    iHoraMax = 0;
                } else {

    				int diaDaSemana = dataLimite.get(Calendar.DAY_OF_WEEK);

    				// Pega o turno de acordo com o dia da semana
    				Map<String, String> turno = getTurno(diaDaSemana);

//					horaEntrada = turno.getHoraEntrada();
//					horaSaida = turno.getHoraSaida();
					horaEntrada = Integer.parseInt(turno.get("HORA_ENTRADA"));
					horaSaida = Integer.parseInt(turno.get("HORA_SAIDA"));
                	

                    //TODO ira trabalhar em segundos
                    horaEntrada *= 60;
                    horaSaida *= 60;                    
                    
                    iHoraMax      = horaSaida - horaEntrada;

                    if (iHoraMax < 0) iHoraMax = 0; //horas máximas disponíveis no dia
                }

                iHoraSobra -= iHoraMax;
                if(iHoraSobra <= 0) iHoraFinal = iHoraSobra + iHoraMax;
            }

            if (bAux == true) {  //a finalizacao será no dia de abertura
                iHoraFinal += iHoraDia; //hora final em minutos
            } else {
                iHoraFinal += horaEntrada; //hora final em minutos
            }

            //TODO Separa o tempo em hora, minuto e segundos
    		int hora = (int)iHoraFinal / 3600,
    		restante = (int)iHoraFinal % 3600;
    		int minuto = restante / 60;
    		int segundo = restante % 60;             

            //TODO ira trabalhar em segundos
            dataLimite.set(dataLimite.get(Calendar.YEAR), dataLimite.get(Calendar.MONTH), dataLimite.get(Calendar.DATE), hora, minuto, segundo); //seta a hora, minutos e segundos que serão utilizados
        }

        return dataLimite;
    }    

	private boolean isFeriado(String diaMes, String ano) {
		
		// FA = Feriado Anual
		if(feriados != null && feriados.size() > 0){
			
			// Testa se eh um Feriado Anual e/ou um Feriado
			// FA1225 - Natal (Feriado Anual)
			return (feriados.contains("FA" + diaMes) ? true : feriados.contains(ano+diaMes));
		}
		
		return false;
	}		
	
	private Map<String,String> getTurno(int diaDaSemana) {
		
		return turnos.get(String.valueOf(diaDaSemana));
	}
	
	/**
	 * Converte o tempo (HH:mm:ss) para segundos
	 * @param tempo
	 * @return segundos
	 */
	public static long getSegundos(String tempo){
		
		if(tempo == null || tempo.equals(""))
			return 0;
		
		String[] partes = tempo.split(":");
		
		long hora = Long.valueOf(partes[0]);
		hora = (hora > 0) ? (hora * 3600) : 0;
		
		long minuto = Long.valueOf(partes[1]);
		minuto = (minuto > 0) ? (minuto * 60) : 0;
		
		long segundo = 0l;
		
		// Em caso de não haver segundos.
		if (partes.length > 2){
			segundo = Long.valueOf(partes[2]);
		}
		
		return (hora + minuto + segundo);
	}	
	
	public static long getMilisegundos(String tempo){

		return getSegundos(tempo) * 1000;

	}
	
	public Calendar getCalculaTempoAtrasoSegundos(Long tempoAdicionado, String identificadorDH){
		Calendar dataSomada = Calendar.getInstance();
		dataSomada.setTime(dataGravacao.getTime());
	
		long transformaDiasLong = 0;

		long numero = tempoAdicionado;
		if(identificadorDH.equals("D")){
			transformaDiasLong = (numero*10)*3600;
		}
		
		if(identificadorDH.equals("H")){
			transformaDiasLong = numero*3600;
		}
		tempoAtraso = transformaDiasLong;
		//System.out.println(" transformaDiasLong = "+transformaDiasLong+" formatado = "+RelatorioSLAUtil.formataSegundos(transformaDiasLong));
		Calendar dataLimite = getDataLimite();
		return dataLimite;
	}
	
	
	public Calendar getCalculaTempoAtraso(Long tempoAdicionado){
		tempoAtraso = tempoAdicionado;
		//System.out.println(" transformaDiasLong = "+tempoAdicionado+" formatado = "+RelatorioSLAUtil.formataSegundos(tempoAdicionado));
		Calendar dataLimite = getDataLimite();
		return dataLimite;
	}
    
	public static Calendar getDiaUtil(Calendar data, List<String> diasNaoTrabalhadosList,Map<String, Map<String, String>> turnoSemanaMap){
			
			Calendar retorno = Calendar.getInstance();			
			try {
				
				while(validaData(data,diasNaoTrabalhadosList,turnoSemanaMap)){
					data = retornaData(data,diasNaoTrabalhadosList,turnoSemanaMap);
				}
				
				retorno = data;
				} catch (Exception e) {
				
				throw e;
			}
			return retorno;
	}
		
	public static boolean validaData(Calendar data,List<String> diasNaoTrabalhadosList,Map<String,Map<String, String>> turnoSemanaMap){
			
			//logger.debug(" validaData = "+data.getTime());
			boolean validacaoData = false;
			int diaSemana = data.get(Calendar.DAY_OF_WEEK);
			
			Map<String,String> dia = turnoSemanaMap.get(String.valueOf(diaSemana));
			long horaEntrada   =  Integer.parseInt(dia.get("HORA_ENTRADA"));
			
			if(horaEntrada > 0){
				SimpleDateFormat dateFormat 	= new SimpleDateFormat("dd/MM/yyyy");
				String sDataAux = dateFormat.format(data.getTime());
				if(diasNaoTrabalhadosList.contains(sDataAux)) {
					validacaoData = true;			
				}
			}else{
				validacaoData = true;
			}

			//logger.debug(" validacaoData = "+validacaoData);
			//System.out.println("validaData retorno = "+validacaoData);
			return validacaoData;
	}
		
	public static Calendar retornaData(Calendar data,List<String> diasNaoTrabalhadosList,Map<String,Map<String, String>> turnoSemanaMap){
			
			//logger.debug(" retornaData = "+data.getTime());
			Calendar dataNova = Calendar.getInstance();
			int diaSemana = data.get(Calendar.DAY_OF_WEEK);
			
			SimpleDateFormat dateFormat 	= new SimpleDateFormat("dd/MM/yyyy");
			String sDataAux = dateFormat.format(data.getTime());
			//System.out.println("retornaData dataChegou = "+data.getTime());
			//if(diasNaoTrabalhadosList.contains(sDataAux)) {
			Map<String,String> dia = turnoSemanaMap.get(String.valueOf(diaSemana));
			long horaEntrada   =  Integer.parseInt(dia.get("HORA_ENTRADA"));
		
			if(horaEntrada == 0 && !diasNaoTrabalhadosList.contains(sDataAux)){
				if(diaSemana==1){ //Domingo
					data.add(Calendar.DAY_OF_MONTH, 1);
				
				} else if(diaSemana==7){ //Sábado
					data.add(Calendar.DAY_OF_MONTH, 2);
				}
			}
			
			
			sDataAux = dateFormat.format(data.getTime());
			if(diasNaoTrabalhadosList.contains(sDataAux)) {
				data.add(Calendar.DAY_OF_MONTH, 1);			
			}
			
			dataNova = data;
			//System.out.println("retornaData dataNova = "+dataNova.getTime());
			//logger.debug(" retornaData  dataNova = "+dataNova.getTime());
			return dataNova;
	}
}




