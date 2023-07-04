package com.lecom.workflow.cadastros.common.util;

import static br.com.lecom.api.factory.ECMFactory.documento;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;

import org.apache.log4j.Logger;

/**
 * Classe responsavel por agregar os métodos que tratam arquivos do ECM
 * 
 * @author Mauricio Moura
 * @data Junho 2016
 * @version 1
 *
 */
public class UtilECM {

	private static final Logger logger = Logger.getLogger(Funcoes.class);

	/**
	 * Transforma dado recuperado em arquivo para anexo
	 * 
	 * @param campoAnexo Conteúdo do campo do BPM que guarda a referência do
	 *                   template
	 * @return InputStream com do arquivo
	 * 
	 *         Ex : InputStream anexoEmail =
	 *         UtilECM.getWFFilePath(parametros.get("$OFICIO_ENTRADA_01"));
	 */
	public static InputStream getWFFilePath(String campoAnexo) throws Exception {

		InputStream is = null;

		if (campoAnexo != null && !"".equals(campoAnexo)) {
			String[] caminhos = campoAnexo.split("[:]");
			String caminhoFisico = caminhos[1];

			logger.debug(" caminhoFisico = " + caminhoFisico);
			is = documento().lerArquivo(caminhoFisico);
		}

		return is;
	}

	/**
	 * Busca nome original do arquivo, com extensão
	 * 
	 * @param campoAnexo Conteúdo do campo do BPM que guarda a referência do
	 *                   template
	 * @return String com o nome e extensão do arquivo
	 * 
	 *         Ex : String nomeAnexo =
	 *         UtilECM.buscaNomeAnexo(parametros.get("$OFICIO_ENTRADA_01"));
	 */
	public static String buscaNomeAnexo(String anexo) throws Exception {

		String nomeArquivo = "";

		if (anexo != null && !"".equals(anexo)) {
			String[] caminhos = anexo.split("[:]");
			nomeArquivo = caminhos[0];
		}

		return nomeArquivo;
	}

	/**
	 * Grava cópia do arquivo do ECM em disco. Por default o arquivo será gravado na
	 * pasta temp do tomcat pois poderá ser excluído depois, caso precise de um
	 * local diferente chame o método getOutputStreamDiretorio
	 * 
	 * @param input     : InputStream gerado no método getWFFilePath
	 * @param nomeAnexo : String gerado no método buscaNomeAnexo
	 * @return InputStream com o arquivo
	 * 
	 *         Ex : String caminhoAnexo = UtilECM.getOutputStream(anexoEmail,
	 *         nomeAnexo);
	 */
	public static String getOutputStream(InputStream input, String nomeAnexo) throws Exception {
		return getOutputStreamDiretorio(input, null, nomeAnexo);
	}

	/**
	 * Grava cópia do arquivo do ECM em disco. Por default o arquivo será gravado na
	 * pasta temp do tomcat pois poderá ser excluído depois, caso precise de um
	 * local diferente preencha o parâmetro
	 * 
	 * @param input         : InputStream gerado no método getWFFilePath
	 * @param localGravavao : String com endereço onde o arquivo deve ser gravado
	 * @param nomeAnexo     : String gerado no método buscaNomeAnexo
	 * @return InputStream com o arquivo
	 * 
	 *         Ex : String caminhoAnexo = UtilECM.getOutputStream(anexoEmail,
	 *         nomeAnexo);
	 */
	@SuppressWarnings("unlikely-arg-type")
	public static String getOutputStreamDiretorio(InputStream input, String localGravavao, String nomeAnexo)
			throws Exception {

		String dirAnexoFull = "";
		if (localGravavao == null) {
			dirAnexoFull = Funcoes.getWFRootDir() + File.separator + ".." + File.separator + ".." + File.separator
					+ "temp" + File.separator + nomeAnexo;
		} else {
			dirAnexoFull = localGravavao + File.separator + nomeAnexo;
		}

		OutputStream output = null;

		try {

			if (input != null && !"".equals(input)) {

				// cria arquivos de anexo temporarios
				output = new FileOutputStream(new File(dirAnexoFull));
				int read = 0;
				byte[] bytes = new byte[1024];

				while ((read = input.read(bytes, 0, bytes.length)) > 0) {
					output.write(bytes, 0, read);
					output.flush();
				}
			}

		} catch (Exception e) {

			if (input != null) {

				try {
					input.close();
				} catch (Exception e1) {
					e.printStackTrace();
					logger.error("ERRO AO FECHAR INPUT: " + e1);
				}
			}

			if (output != null) {

				try {
					output.close();
				} catch (Exception e2) {
					e.printStackTrace();
					logger.error("ERRO AO FECHAR OUTPUT: " + e2);
				}
			}
		}

		return dirAnexoFull;
	}

}
