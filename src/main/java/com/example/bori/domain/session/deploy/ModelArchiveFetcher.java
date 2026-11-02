package com.example.bori.domain.session.deploy;

import com.example.bori.global.github.GithubRepoUrl;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.DigestOutputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.util.HexFormat;
import java.util.regex.Pattern;
import java.util.zip.ZipFile;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

// 공개 GitHub 저장소의 기본 브랜치 zip을 임시 파일로 받는다 (규격 §6).
// 학생이 입력한 크기는 믿지 않고, 받으면서 보드 업로드 한도를 넘는지 센다.
@Slf4j
@Component
public class ModelArchiveFetcher {

    // zip 최상위 폴더({repo}-HEAD/) 바로 아래 bori.yaml
    private static final Pattern MANIFEST = Pattern.compile("^[^/]+/bori\\.ya?ml$");
    private static final long MB = 1024 * 1024;

    private final String codeloadBaseUrl;
    private final HttpClient httpClient = HttpClient.newBuilder()
            .followRedirects(HttpClient.Redirect.NORMAL)
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    public ModelArchiveFetcher(@Value("${bori.github.codeload-base-url:https://codeload.github.com}") String codeloadBaseUrl) {
        this.codeloadBaseUrl = codeloadBaseUrl.replaceAll("/+$", "");
    }

    public record FetchedArchive(Path path, long sizeBytes, String sha256) {

        public int sizeMbCeil() {
            return (int) Math.max(1, (sizeBytes + MB - 1) / MB);
        }
    }

    public FetchedArchive fetch(GithubRepoUrl repo, int limitMb) throws DeployFailure {
        URI uri = URI.create(codeloadBaseUrl + "/" + repo.owner() + "/" + repo.repo() + "/zip/HEAD");
        Path file = null;
        try {
            file = Files.createTempFile("bori-model-", ".zip");
            HttpRequest request = HttpRequest.newBuilder(uri).timeout(Duration.ofMinutes(5)).GET().build();
            HttpResponse<InputStream> response = httpClient.send(request, HttpResponse.BodyHandlers.ofInputStream());
            try (InputStream body = response.body()) {
                if (response.statusCode() == 404) {
                    throw new DeployFailure("저장소를 찾을 수 없습니다. 공개 저장소인지 확인하세요.");
                }
                if (response.statusCode() != 200) {
                    throw new DeployFailure("GitHub에서 저장소를 받지 못했습니다 (HTTP " + response.statusCode() + ").");
                }
                MessageDigest digest = MessageDigest.getInstance("SHA-256");
                long size = copyWithLimit(body, file, digest, limitMb * MB);
                if (size < 0) {
                    throw new DeployFailure("저장소 크기가 업로드 한도 " + limitMb + " MB를 넘습니다.");
                }
                checkManifest(file);
                return new FetchedArchive(file, size, HexFormat.of().formatHex(digest.digest()));
            }
        } catch (DeployFailure e) {
            deleteQuietly(file);
            throw e;
        } catch (IOException | NoSuchAlgorithmException e) {
            deleteQuietly(file);
            log.warn("모델 저장소 다운로드 실패 {}: {}", uri, e.toString());
            throw new DeployFailure("GitHub에서 저장소를 받지 못했습니다.");
        } catch (InterruptedException e) {
            deleteQuietly(file);
            Thread.currentThread().interrupt();
            throw new DeployFailure("배포가 중단되었습니다.");
        }
    }

    // 한도를 넘으면 -1
    private long copyWithLimit(InputStream in, Path file, MessageDigest digest, long limitBytes) throws IOException {
        long total = 0;
        byte[] buffer = new byte[64 * 1024];
        try (OutputStream out = new DigestOutputStream(Files.newOutputStream(file), digest)) {
            int n;
            while ((n = in.read(buffer)) != -1) {
                total += n;
                if (total > limitBytes) {
                    return -1;
                }
                out.write(buffer, 0, n);
            }
        }
        return total;
    }

    private void checkManifest(Path file) throws DeployFailure {
        try (ZipFile zip = new ZipFile(file.toFile())) {
            boolean found = zip.stream().anyMatch(entry -> MANIFEST.matcher(entry.getName()).matches());
            if (!found) {
                throw new DeployFailure("저장소 최상위에 bori.yaml이 없습니다.");
            }
        } catch (IOException e) {
            throw new DeployFailure("받은 파일이 올바른 zip이 아닙니다.");
        }
    }

    private void deleteQuietly(Path file) {
        if (file != null) {
            try {
                Files.deleteIfExists(file);
            } catch (IOException ignored) {
                // 임시 파일
            }
        }
    }
}
