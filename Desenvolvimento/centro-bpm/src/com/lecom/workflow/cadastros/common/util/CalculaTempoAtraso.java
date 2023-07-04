package com.lecom.workflow.cadastros.common.util;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Map;

public class CalculaTempoAtraso {

	private Calendar dataLimite;
	private Calendar dataAtual;
	private Map<String, Map<String, String>> turnos;
	private List<String> feriados;

	private SimpleDateFormat formataAno = new SimpleDateFormat("yy");
	private SimpleDateFormat formataDiaMes = new SimpleDateFormat("MMdd");
	private int totalHorasLimite;
	private int totalHorasAtual;	
	
	public CalculaTempoAtraso(Calendar dataLimite, Calendar dataGravacaoEtapa, Map<String, Map<String, String>> turnos, List<String> feriados) {
		
		this.dataLimite = Calendar.getInstance();
		this.dataLimite.setTime(dataLimite.getTime());
		this.dataAtual = Calendar.getInstance();
		this.dataAtual.setTime(dataGravacaoEtapa.getTime());
		this.turnos = turnos;
		this.feriados = feriados;
		
		calculaTotalHorasAtual();
		calculaTotalHorasLimite();
	}
	
	
	public boolean estaNoLimite() {
		return isMesmaHora() ? true : dataAtual.before(dataLimite);
	}

	private boolean isMesmaHora() {
		return dataAtual.equals(dataLimite);
	}

	public int getDiferencaEmDias() {

		// Data Atual
		Calendar dataAtualAux = Calendar.getInstance();
		
		String fmt1 = new SimpleDateFormat("dd/MM/yyyy").format(dataAtual.getTime());
		Date dt1 = null;
		try {
			dt1 = new SimpleDateFormat("dd/MM/yyyy").parse(fmt1);
		} catch (ParseException e) {
			e.printStackTrace();
		}
		
		dataAtualAux.setTime(dt1);
		dataAtualAux.set(Calendar.SECOND, 1);
		dataAtualAux.set(Calendar.MINUTE, 0);
		dataAtualAux.set(Calendar.HOUR_OF_DAY, 0);		
		
		// Data Limite
		Calendar dataLimiteAux = Calendar.getInstance();
		
		String fmt2 = new SimpleDateFormat("dd/MM/yyyy").format(dataLimite.getTime());
		Date dt2 = null;
		try {
			dt2 = new SimpleDateFormat("dd/MM/yyyy").parse(fmt2);
		} catch (ParseException e) {
			e.printStackTrace();
		}
		
		dataLimiteAux.setTime(dt2);
		dataLimiteAux.set(Calendar.SECOND, 1);
		dataLimiteAux.set(Calendar.MINUTE, 0);
		dataLimiteAux.set(Calendar.HOUR_OF_DAY, 0);		
		
		long diferenca = dataAtualAux.getTimeInMillis() - dataLimiteAux.getTimeInMillis();
		
		return -(int)(diferenca/1000/60/60/24);

	}
	
	public Long getTotalEmSegundos(){
		
		return (estaNoLimite() ? calculaTempoPositivo() : calculaTempoNegativo());
	}
	
	public Long getTotalEmMilisegundos(){
		
		return getTotalEmSegundos() * 1000;
	}	
	
	public Long getTotalConsumidoEmSegundos(){
		
		return calculaTempoConsumido();
	}
	
	public Long getTotalConsumidoEmMilisegundos(){
		
		return getTotalConsumidoEmSegundos() * 1000;
	}

	private Long calculaTempoPositivo() {
		
		if(isMesmaHora()){
			return 0L;
		}
		else{
			
			int diferencaEmDias = getDiferencaEmDias();
			
			if(diferencaEmDias > 0){
				
				int indice = 0;
				Long totalHoras = 0L;
				
				do {
					
					// Testa se eh um feriado
					if(!isFeriado(formataDiaMes.format(dataAtual.getTime()), formataAno.format(dataAtual.getTime()))){

						int horaInicial = 0;
						int horaFinal = 0;
						int horaSaida = 0;
						int horaEntrada = 0;
						int diaDaSemana = dataAtual.get(Calendar.DAY_OF_WEEK);

						// Pega o turno de acordo com o dia da semana
						Map<String, String>  turno = getTurno(diaDaSemana);


//						horaEntrada = turno.getHoraEntrada();
//						horaSaida = turno.getHoraSaida();
						horaEntrada = Integer.parseInt(turno.get("HORA_ENTRADA"));
						horaSaida = Integer.parseInt(turno.get("HORA_SAIDA"));
						
						 //TODO ira trabalhar em segundos
						horaEntrada *= 60;
						horaSaida *= 60;
						
						if (indice == 0) {
							
							horaInicial = (totalHorasAtual < horaSaida) ? totalHorasAtual : horaSaida;	
						}
						else{
							
							horaInicial = horaEntrada;
							
						}
						
						if(indice == diferencaEmDias){
							
							horaFinal = (totalHorasLimite < horaSaida) ? totalHorasLimite :horaSaida;
						} 
						else{
							horaFinal = horaSaida;
						}
						
						totalHoras += (horaFinal - horaInicial);
						
					}
					
					indice++;
					dataAtual.add(Calendar.DATE, 1);
					
				} while (indice <= diferencaEmDias);
				
				//TODO ira trabalhar em segundos
				//return (totalHoras * new Long(60 * 1000));
				return (totalHoras);
			}
			else{

				return getTempoMesmoDia();
			}
			
		}
		
	}
	
	
	private Long calculaTempoConsumido() {
		
		if(isMesmaHora()){
			return 0L;
		}
		else{
			
			int diferencaEmDias = getDiferencaEmDias();
			if(diferencaEmDias<0){
				diferencaEmDias = -diferencaEmDias;
			}
			
			//if(diferencaEmDias > 0){
				
				int indice = 0;
				Long totalHoras = 0L;
				
				do {
					
					// Testa se eh um feriado
					if(!isFeriado(formataDiaMes.format(dataAtual.getTime()), formataAno.format(dataAtual.getTime()))){

						int horaInicial = 0;
						int horaFinal = 0;
						int horaSaida = 0;
						int horaEntrada = 0;
						int diaDaSemana = dataAtual.get(Calendar.DAY_OF_WEEK);

						// Pega o turno de acordo com o dia da semana
						Map<String, String>  turno = getTurno(diaDaSemana);


//						horaEntrada = turno.getHoraEntrada();
//						horaSaida = turno.getHoraSaida();
						horaEntrada = Integer.parseInt(turno.get("HORA_ENTRADA"));
						horaSaida = Integer.parseInt(turno.get("HORA_SAIDA"));
						
						 //TODO ira trabalhar em segundos
						horaEntrada *= 60;
						horaSaida *= 60;
						
						if (indice == 0) {
							
							horaInicial = (totalHorasAtual < horaSaida) ? totalHorasAtual : horaSaida;	
						}
						else{
							
							horaInicial = horaEntrada;
							
						}
						
						if(indice == diferencaEmDias){
							
							horaFinal = (totalHorasLimite < horaSaida) ? totalHorasLimite :horaSaida;
						}
						else{
							horaFinal = horaSaida;
						}
						
						totalHoras += (horaFinal - horaInicial);
						
						
					}
					
					indice++;
					dataAtual.add(Calendar.DATE, 1);
					
				} while (indice <= diferencaEmDias);
				if(totalHoras < 0){
					totalHoras = -totalHoras;
				}
				//TODO ira trabalhar em segundos
				//return (totalHoras * new Long(60 * 1000));
				return (totalHoras);
			//}
			//else{

			//	return -getTempoMesmoDia();
			//}
			
		}
		
	}

	private Long calculaTempoNegativo() {
		
		int diferencaEmDias = -getDiferencaEmDias();
		
		if(diferencaEmDias > 0){

			int indice = 0;
			Long totalHoras = 0L;
			
			do {
				
				// Testa se eh um feriado
				if(!isFeriado(formataDiaMes.format(dataLimite.getTime()), formataAno.format(dataLimite.getTime()))){

					int horaInicial = 0;
					int horaFinal = 0;
					int horaSaida = 0;
					int horaEntrada = 0;
					int diaDaSemana = dataLimite.get(Calendar.DAY_OF_WEEK);

					// Pega o turno de acordo com o dia da semana
					Map<String, String>  turno = getTurno(diaDaSemana);


//					horaEntrada = turno.getHoraEntrada();
//					horaSaida = turno.getHoraSaida();
					horaEntrada = Integer.parseInt(turno.get("HORA_ENTRADA"));
					horaSaida = Integer.parseInt(turno.get("HORA_SAIDA"));
					
					 //TODO ira trabalhar em segundos
					horaEntrada *= 60;
					horaSaida *= 60;					
					
					if (indice == 0) {
						
						horaInicial = (totalHorasLimite < horaSaida) ? totalHorasLimite : horaSaida;	
					}
					else{
						
						horaInicial = horaEntrada;
						
					}
					
					if(indice == diferencaEmDias){
						
						horaFinal = (totalHorasAtual < horaSaida) ? totalHorasAtual :horaSaida;
					}
					else{
						horaFinal = horaSaida;
					}
					
					totalHoras += (horaFinal - horaInicial);
					
				}
				
				indice++;
				dataLimite.add(Calendar.DATE, 1);
				
			} while (indice <= diferencaEmDias);
			
			//TODO ira trabalhar em segundos
			//return -(totalHoras * new Long(60 * 1000));
			return -(totalHoras);
		
		}
		else{
			
			return getTempoMesmoDia();
		}		

	}
	
	private void calculaTotalHorasLimite() {
		
		// Calcula o total de horas da gravacao
		int dataLimiteHoraDia = this.dataLimite.get(Calendar.HOUR_OF_DAY);
		int dataLimiteMinuto = this.dataLimite.get(Calendar.MINUTE);
		
		//TODO ira trabalhar em segundos
		int dataLimiteSegundos = this.dataLimite.get(Calendar.SECOND);
		
		totalHorasLimite = (dataLimiteHoraDia * 3600) + (dataLimiteMinuto * 60) + dataLimiteSegundos;
		
	}	

	private void calculaTotalHorasAtual() {
		
		// Calcula o total de horas da data atual
		int dataAtualHoraDia = this.dataAtual.get(Calendar.HOUR_OF_DAY);
		int dataAtualMinuto = this.dataAtual.get(Calendar.MINUTE);
		
		//TODO ira trabalhar em segundos
		int dataAtualMinutoSegundos = this.dataAtual.get(Calendar.SECOND);
		
		totalHorasAtual = (dataAtualHoraDia * 3600) + (dataAtualMinuto * 60) + dataAtualMinutoSegundos;
	}	
	
	private Long getTempoMesmoDia() {
		
		//TODO ira trabalhar em segundos
		//return(totalHorasLimite - totalHorasAtual) * new Long(60 * 1000);
		return (long)(totalHorasLimite - totalHorasAtual);
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

}
