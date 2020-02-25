#!/bin/sh
":"; exec emacs --quick --script "$0" -- "$@" # -*- mode: emacs-lisp; lexical-binding: t; -*-
(setq debug-on-error t)
(defun done ()
  (kill-emacs 0))

(require 'org)

(setq help-message "
Generate HTML File from Org File
  generate-html.el <org-file>")


(defun print-help-message ()
  (message "%s" help-message))


;; Make sure we have the file argument
(when (not (= (length argv) 2))
  (message "ERROR: Not enough commandline arguments: %s" (cdr argv))
  (print-help-message)
  (done))


;; Check to make sure the file exists
(let ((filename (car (cdr argv))))
  (when (not (file-exists-p filename))
    (message "ERROR: Given file does not exist: %s" filename)
    (print-help-message)
    (done)))

;; Configure Emacs to handle Image Exports correctly

(defun org-custom-link-img-follow (path)
  (org-open-file-with-emacs
   (format "%s" path)))


(defun org-custom-link-img-export (path desc format)
  "Files pulled from the /docs/images folder have their relative
links fixed for the public website."
  (let ((directory (file-name-directory path))
        (filename (file-name-nondirectory path)))
    (cond
     
     ;; Fix relative links to ../images folder
     ((and (eq format 'html)
           (string= directory "../images/"))
      (format "<img src=\"/images/%s\" alt=\"%s\"/>" filename desc))

     ;; Default html export for paths outside of ../images folder
     ((eq format 'html)
      (format "<img src=\"%s\" alt=\"%s\"/>" path desc)))))

(org-link-set-parameters
 "img"
 :follow 'org-custom-link-img-follow
 :export 'org-custom-link-img-export)


;;
;; BEGIN EXPORT
;;

(let ((filename (car (cdr argv))))
  (find-file filename)
  (org-html-export-to-html))


(done)
