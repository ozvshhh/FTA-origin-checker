package com.tradedoc.origincheck.check.storage;

import com.tradedoc.origincheck.common.ApiException;
import com.tradedoc.origincheck.common.ErrorCode;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.io.IOUtils;
import org.apache.pdfbox.multipdf.PDFMergerUtility;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.encryption.InvalidPasswordException;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/** 여러 PDF 를 올린 순서대로 하나로 이어 붙이고, 파일마다 쪽 수를 센다. */
@Component
public class PdfBundler {

    /** 쪽 수. 열 수 없거나 암호가 걸린 PDF 는 INVALID_FILE */
    public int pageCount(Path pdf, String fileName) {
        try (PDDocument d = Loader.loadPDF(pdf.toFile())) {
            int n = d.getNumberOfPages();
            if (n == 0) {
                throw new ApiException(ErrorCode.INVALID_FILE, "쪽이 없는 PDF입니다: " + fileName);
            }
            return n;
        } catch (InvalidPasswordException e) {
            throw new ApiException(ErrorCode.INVALID_FILE, "암호가 걸린 PDF는 열 수 없습니다: " + fileName);
        } catch (IOException e) {
            throw new ApiException(ErrorCode.INVALID_FILE, "PDF 파일을 열 수 없습니다: " + fileName);
        }
    }

    /** sources 를 순서대로 이어 붙여 target 에 쓴다 */
    public void merge(List<Path> sources, Path target) {
        PDFMergerUtility merger = new PDFMergerUtility();
        try (OutputStream out = Files.newOutputStream(target)) {
            for (Path p : sources) {
                merger.addSource(p.toFile());
            }
            merger.setDestinationStream(out);
            merger.mergeDocuments(IOUtils.createTempFileOnlyStreamCache());
        } catch (IOException e) {
            throw new UncheckedIOException("PDF 이어 붙이기 실패", e);
        }
    }
}
