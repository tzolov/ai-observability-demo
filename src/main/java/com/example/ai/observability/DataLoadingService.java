package com.example.ai.observability;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.document.Document;
import org.springframework.ai.reader.ExtractedTextFormatter;
import org.springframework.ai.reader.pdf.PagePdfDocumentReader;
import org.springframework.ai.reader.pdf.config.PdfDocumentReaderConfig;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import java.util.List;

@Service
public class DataLoadingService {

	private static final Logger logger = LoggerFactory.getLogger(DataLoadingService.class);

	@Value("classpath:/data/medicaid-wa-faqs.pdf")
	private Resource pdfResource;

	private final VectorStore vectorStore;

	@Autowired
	public DataLoadingService(VectorStore vectorStore) {
		Assert.notNull(vectorStore, "VectorStore must not be null.");
		this.vectorStore = vectorStore;
	}

	public void load() {
		// Extract
		PagePdfDocumentReader pdfReader = new PagePdfDocumentReader(this.pdfResource,
				PdfDocumentReaderConfig.builder()
					.withPageExtractedTextFormatter(ExtractedTextFormatter.builder()
						.withNumberOfBottomTextLinesToDelete(3)
						.withNumberOfTopPagesToSkipBeforeDelete(1)
						.build())
					.withPagesPerDocument(1)
					.build());
		// Transform
		var tokenTextSplitter = new TokenTextSplitter();

		logger.info(
				"Parsing document, splitting, creating embeddings and storing in vector store...");

		List<Document> splitDocuments = tokenTextSplitter.split(pdfReader.read());

		// tag as external knowledge in the vector store's metadata
		for (Document splitDocument : splitDocuments) {
			splitDocument.getMetadata().put("filename", pdfResource.getFilename());
			splitDocument.getMetadata().put("version", 1);
		}

		// Load
		this.vectorStore.write(splitDocuments);

		logger.info("Done parsing document, splitting, creating embeddings and storing in vector store");

	}

}
