package main

import "C"
import (
	"fmt"
	"io"
	"net/http"
	"net/url"
	"os"
	"path/filepath"
	"strings"
)


func GoDownloadFile(url *C.char, savePath *C.char) C.int {
	if err := downloadFile(C.GoString(url), C.GoString(savePath)); err != nil {
		return 0
	}
	return 1
}

func GoGetRedirectInfo(url *C.char) *C.char {
	finalURL, filename, err := getRedirectInfo(C.GoString(url))
	if err != nil {
		return C.CString("")
	}
	
	result := fmt.Sprintf("%s|%s", finalURL, filename)
	return C.CString(result)
}

func getRedirectInfo(urlStr string) (string, string, error) {
	client := &http.Client{
		CheckRedirect: func(req *http.Request, via []*http.Request) error {
			if len(via) >= 10 {
				return fmt.Errorf("too many redirects")
			}
			return nil
		},
	}

	resp, err := client.Get(urlStr)
	if err != nil {
		return "", "", fmt.Errorf("request failed: %w", err)
	}
	defer resp.Body.Close()

	finalURL := resp.Request.URL.String()
	
	filename := getFilenameFromResponse(resp)
	
	if filename == "" || filename == "unknown" {
		filename = getFilenameFromURL(finalURL)
	}

	return finalURL, filename, nil
}

func getFilenameFromResponse(resp *http.Response) string {
	contentDisposition := resp.Header.Get("Content-Disposition")
	if contentDisposition != "" {
		parts := strings.Split(contentDisposition, ";")
		for _, part := range parts {
			part = strings.TrimSpace(part)
			if strings.HasPrefix(part, "filename=") {
				filename := strings.TrimPrefix(part, "filename=")
				filename = strings.Trim(filename, `"`)
				return filename
			}
			if strings.HasPrefix(part, "filename*=") {
				filename := strings.TrimPrefix(part, "filename*=")
				if idx := strings.Index(filename, "''"); idx != -1 {
					filename = filename[idx+2:]
				}
				filename = strings.Trim(filename, `"`)
				decoded, err := url.QueryUnescape(filename)
				if err == nil {
					return decoded
				}
				return filename
			}
		}
	}
	
	return getFilenameFromURL(resp.Request.URL.String())
}

func getFilenameFromURL(urlStr string) string {
	parsedURL, err := url.Parse(urlStr)
	if err != nil {
		return "unknown"
	}

	path := parsedURL.Path
	filename := filepath.Base(path)

	if filename == "" || filename == "/" || filename == "." {
		query := parsedURL.Query()
		if name := query.Get("filename"); name != "" {
			return name
		}
		return "download"
	}

	return filename
}

func downloadFile(url, savePath string) error {
	dir := filepath.Dir(savePath)
	if err := os.MkdirAll(dir, 0755); err != nil {
		return fmt.Errorf("mkdir failed: %w", err)
	}

	resp, err := http.Get(url)
	if err != nil {
		return fmt.Errorf("request failed: %w", err)
	}
	defer resp.Body.Close()

	if resp.StatusCode != http.StatusOK {
		return fmt.Errorf("http error: %d", resp.StatusCode)
	}

	file, err := os.Create(savePath)
	if err != nil {
		return fmt.Errorf("create file failed: %w", err)
	}
	defer file.Close()

	_, err = io.Copy(file, resp.Body)
	return err
}