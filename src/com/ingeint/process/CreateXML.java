package com.ingeint.process;

import java.io.File;
import java.io.FileWriter;
import java.util.List;
import java.util.logging.Level;

import org.compiere.model.MOrgInfo;
import org.compiere.model.Query;
import org.compiere.process.ProcessInfoParameter;
import org.eevolution.model.MHRMovement;
import org.eevolution.model.MHRProcess;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.output.Format;
import org.jdom2.output.XMLOutputter;

import com.ingeint.base.CustomProcess;

public class CreateXML extends CustomProcess {

	int p_HR_Concept_ID = 0;

	protected void prepare() {
		for (ProcessInfoParameter para : getParameter()) {
			String name = para.getParameterName();
			if (name.equals("HR_Concept_ID"))
				p_HR_Concept_ID = para.getParameterAsInt();
			else
				log.log(Level.SEVERE, "Unknown Parameter: " + name);
		}
	}

	@Override
	protected String doIt() throws Exception {

		MHRProcess process = new MHRProcess(getCtx(), getRecord_ID(), get_TrxName());
		MOrgInfo orgInfo = MOrgInfo.get(process.getAD_Org_ID());

		// Root Element
		Element root = new Element("RelacionRetencionesISLR");
		root.setAttribute("RifAgente", orgInfo.getTaxID());
		root.setAttribute("Periodo", process.getDateAcct().toString().substring(0, 8).replace("-",""));
		
		

		List<MHRMovement> movements = new Query(getCtx(), MHRMovement.Table_Name,
				"HR_Process_ID = ? AND HR_Concept_ID = ? AND AD_Client_ID = ? ", get_TrxName())
						.setParameters(new Object[] { getRecord_ID(), p_HR_Concept_ID, getAD_Client_ID() })
						.list();

		for (MHRMovement move : movements) {

			// Element 1
			Element detail = new Element("DetalleRetencion");
			detail.addContent(new Element("RifRetenido").addContent("V" + move.getC_BPartner().getTaxID()));
			detail.addContent(new Element("NumeroFactura").addContent("0"));
			detail.addContent(new Element("NumeroControl").addContent("NA"));
			detail.addContent(new Element("FechaOperacion").addContent(move.getHR_Process().getDateAcct().toString().substring(0, 10).replace("-","")));
			detail.addContent(new Element("CodigoConcepto").addContent("001"));
			detail.addContent(new Element("MontoOperacion").addContent(move.getAmount().toString()));
			detail.addContent(new Element("PorcentajeRetencion").addContent("0"));

			root.addContent(detail);

		}
		
		Document doc = new Document();
		doc.setRootElement(root);

		// Create the XML
		XMLOutputter outter = new XMLOutputter();
		Format format = Format.getPrettyFormat();
	    format.setEncoding("ISO-8859-1");
		outter.setFormat(format);		
		File file = new File(getRecord_ID() + ".xml");
		outter.output(doc, new FileWriter(file));

		processUI.download(file);
		return "Creado: " + file;
	}

}
