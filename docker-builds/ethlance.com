server {

  listen 80 default_server;

  root /ethlance/resources/public/;
  index index.html;

  location / {
    # add_header Cache-Control "no-store";
    expires 1h;
    add_header Cache-Control "public";
    try_files $uri $uri/index.html /index.html;
  }

  location ~ /(contracts|images|assets|js|css|fonts)(.*)$ {
    expires 1h;
    add_header Cache-Control "public";
    rewrite /(contracts|images|assets|js|css|fonts)(.*) /$1$2 break;
    try_files $uri $uri/index.html /index.html;
  }

  location = /X0X.html {
    root /usr/share/nginx/html/;
    internal;
   }

  # redirect server error pages to the static error page
  error_page 400 401 402 403 404 405 406 407 408 409 410 411 412 413 414 415 416 417 418 420 422 423 424 426 428 429 431 444 449 450 451 500 501 502 503 504 505 506 507 508 509 510 511 /X0X.html;

}
