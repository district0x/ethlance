'use strict';

const gulp = require('gulp');
const minifyCSS = require('gulp-clean-css');
const concat = require('gulp-concat');
const uglify = require('gulp-uglify');
const rename = require("gulp-rename");
const htmlmin = require('gulp-htmlmin');
const less = require('gulp-less');
const minifyInline = require('gulp-minify-inline');
const removeHtmlComments = require('gulp-remove-html-comments');
const autoprefixer = require('gulp-autoprefixer');

gulp.task('styles', gulp.series((done) =>{
  gulp.src([
    'src/less/home/styles.less'
  ])
    .pipe(concat('home.min.css'))
    .pipe(less())
    .pipe(autoprefixer())
    .pipe(minifyCSS())
    .pipe(gulp.dest('deploy/css'))
    done();

    gulp.src([
      'src/less/panel/styles.less'
    ])
    .pipe(concat('panel.min.css'))
    .pipe(less())
    .pipe(minifyCSS())
    .pipe(gulp.dest('deploy/css'))
    done();

}));

gulp.task('pages', gulp.series((done) =>{
  gulp.src(['src/**/*.html','src/**/*.php'])
    .pipe(removeHtmlComments())
    .pipe(minifyInline())
    .pipe(htmlmin({collapseWhitespace: true}))
    .pipe(rename({prefix: ''}))
    .pipe(gulp.dest('deploy'))
    done();
}));

gulp.task('js', gulp.series((done) =>{
  gulp.src([
    'src/js/**/*.js'
  ])
	.pipe(uglify())
	.pipe(rename({suffix: '.min'}))
    .pipe(gulp.dest('deploy/js'))
    done();

  gulp.src([
    'src/js/lib/**/*.js',
    './node_modules/jquery/dist/jquery.min.js',
    './node_modules/gsap/src/minified/TweenMax.min.js'
  ])
    .pipe(concat('libs.min.js'))
    .pipe(uglify())
    .pipe(rename({prefix: ''}))
    .pipe(gulp.dest('deploy/js'))
    done();
}));

gulp.task('default', gulp.series((done) =>{
  gulp.watch(['src/less/**/*.less'], gulp.parallel(['styles']));
  gulp.watch(['src/js/**/*.js'], gulp.parallel(['js']));
  gulp.watch(['src/**/*.html','src/**/*.php'], gulp.parallel(['pages']));
  done();
}));

gulp.task('build', gulp.series(['styles', 'pages', 'js']));
//gulp.task('default', gulp.series(['build']));
