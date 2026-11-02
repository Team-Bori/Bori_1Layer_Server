package com.example.bori.global.github;

import com.example.bori.global.error.BusinessException;
import com.example.bori.global.error.ErrorCode;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

// https://github.com/{owner}/{repo} 형식만 받는다. 끝의 .git, / 는 허용한다.
public record GithubRepoUrl(String owner, String repo) {

    private static final Pattern PATTERN =
            Pattern.compile("^https://github\\.com/([A-Za-z0-9-]{1,39})/([A-Za-z0-9._-]{1,100}?)(?:\\.git)?/?$");

    public static GithubRepoUrl parse(String url) {
        if (url == null) {
            throw new BusinessException(ErrorCode.INVALID_GITHUB_URL);
        }
        Matcher m = PATTERN.matcher(url.trim());
        if (!m.matches() || m.group(2).equals(".") || m.group(2).equals("..")) {
            throw new BusinessException(ErrorCode.INVALID_GITHUB_URL);
        }
        return new GithubRepoUrl(m.group(1), m.group(2));
    }

    public String canonical() {
        return "https://github.com/" + owner + "/" + repo;
    }
}
