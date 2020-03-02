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


;;
;; BEGIN EXPORT
;;

(let ((filename (car (cdr argv))))
  (find-file filename)
  (org-html-export-to-html))


(done)
