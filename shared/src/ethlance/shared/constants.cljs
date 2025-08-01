(ns ethlance.shared.constants)


(def countries
  ["United States"
   "Afghanistan"
   "Albania"
   "Algeria"
   "Andorra"
   "Angola"
   "Antigua & Deps"
   "Argentina"
   "Armenia"
   "Australia"
   "Austria"
   "Azerbaijan"
   "Bahamas"
   "Bahrain"
   "Bangladesh"
   "Barbados"
   "Belarus"
   "Belgium"
   "Belize"
   "Benin"
   "Bhutan"
   "Bolivia"
   "Bosnia Herzegovina"
   "Botswana"
   "Brazil"
   "Brunei"
   "Bulgaria"
   "Burkina"
   "Burundi"
   "Cambodia"
   "Cameroon"
   "Canada"
   "Cape Verde"
   "Central African Rep"
   "Chad"
   "Chile"
   "China"
   "Colombia"
   "Comoros"
   "Congo"
   "Congo {Democratic Rep}"
   "Costa Rica"
   "Croatia"
   "Cuba"
   "Cyprus"
   "Czech Republic"
   "Denmark"
   "Djibouti"
   "Dominica"
   "Dominican Republic"
   "East Timor"
   "Ecuador"
   "Egypt"
   "El Salvador"
   "Equatorial Guinea"
   "Eritrea"
   "Estonia"
   "Ethiopia"
   "Fiji"
   "Finland"
   "France"
   "Gabon"
   "Gambia"
   "Georgia"
   "Germany"
   "Ghana"
   "Greece"
   "Grenada"
   "Guatemala"
   "Guinea"
   "Guinea-Bissau"
   "Guyana"
   "Haiti"
   "Honduras"
   "Hungary"
   "Iceland"
   "India"
   "Indonesia"
   "Iran"
   "Iraq"
   "Ireland {Republic}"
   "Israel"
   "Italy"
   "Ivory Coast"
   "Jamaica"
   "Japan"
   "Jordan"
   "Kazakhstan"
   "Kenya"
   "Kiribati"
   "Korea North"
   "Korea South"
   "Kosovo"
   "Kuwait"
   "Kyrgyzstan"
   "Laos"
   "Latvia"
   "Lebanon"
   "Lesotho"
   "Liberia"
   "Libya"
   "Liechtenstein"
   "Lithuania"
   "Luxembourg"
   "Macedonia"
   "Madagascar"
   "Malawi"
   "Malaysia"
   "Maldives"
   "Mali"
   "Malta"
   "Marshall Islands"
   "Mauritania"
   "Mauritius"
   "Mexico"
   "Micronesia"
   "Moldova"
   "Monaco"
   "Mongolia"
   "Montenegro"
   "Morocco"
   "Mozambique"
   "Myanmar, {Burma}"
   "Namibia"
   "Nauru"
   "Nepal"
   "Netherlands"
   "New Zealand"
   "Nicaragua"
   "Niger"
   "Nigeria"
   "Norway"
   "Oman"
   "Pakistan"
   "Palau"
   "Panama"
   "Papua New Guinea"
   "Paraguay"
   "Peru"
   "Philippines"
   "Poland"
   "Portugal"
   "Qatar"
   "Romania"
   "Russian Federation"
   "Rwanda"
   "St Kitts & Nevis"
   "St Lucia"
   "Saint Vincent & the Grenadines"
   "Samoa"
   "San Marino"
   "Sao Tome & Principe"
   "Saudi Arabia"
   "Senegal"
   "Serbia"
   "Seychelles"
   "Sierra Leone"
   "Singapore"
   "Slovakia"
   "Slovenia"
   "Solomon Islands"
   "Somalia"
   "South Africa"
   "South Sudan"
   "Spain"
   "Sri Lanka"
   "Sudan"
   "Suriname"
   "Swaziland"
   "Sweden"
   "Switzerland"
   "Syria"
   "Taiwan"
   "Tajikistan"
   "Tanzania"
   "Thailand"
   "Togo"
   "Tonga"
   "Trinidad & Tobago"
   "Tunisia"
   "Turkey"
   "Turkmenistan"
   "Tuvalu"
   "Uganda"
   "Ukraine"
   "United Arab Emirates"
   "United Kingdom"
   "Uruguay"
   "Uzbekistan"
   "Vanuatu"
   "Vatican City"
   "Venezuela"
   "Vietnam"
   "Yemen"
   "Zambia"
   "Zimbabwe"])


(def language-defs
  [{:name "Abkhaz" :native-name "аҧсуа"}
   {:name "Afar" :native-name "Afaraf"}
   {:name "Afrikaans" :native-name "Afrikaans"}
   {:name "Akan" :native-name "Akan"}
   {:name "Albanian" :native-name "Shqip"}
   {:name "Amharic" :native-name "አማርኛ"}
   {:name "Arabic" :native-name "العربية"}
   {:name "Aragonese" :native-name "Aragonés"}
   {:name "Armenian" :native-name "Հայերեն"}
   {:name "Assamese" :native-name "অসমীয়া"}
   {:name "Avaric" :native-name "авар мацӀ, магӀарул мацӀ"}
   {:name "Avestan" :native-name "avesta"}
   {:name "Aymara" :native-name "aymar aru"}
   {:name "Azerbaijani" :native-name "azərbaycan dili"}
   {:name "Bambara" :native-name "bamanankan"}
   {:name "Bashkir" :native-name "башҡорт теле"}
   {:name "Basque" :native-name "euskara, euskera"}
   {:name "Belarusian" :native-name "Беларуская"}
   {:name "Bengali" :native-name "বাংলা"}
   {:name "Bihari" :native-name "भोजपुरी"}
   {:name "Bislama" :native-name "Bislama"}
   {:name "Bosnian" :native-name "bosanski jezik"}
   {:name "Breton" :native-name "brezhoneg"}
   {:name "Bulgarian" :native-name "български език"}
   {:name "Burmese" :native-name "ဗမာစာ"}
   {:name "Catalan; Valencian" :native-name "Català"}
   {:name "Chamorro" :native-name "Chamoru"}
   {:name "Chechen" :native-name "нохчийн мотт"}
   {:name "Chichewa; Chewa; Nyanja" :native-name "chiCheŵa, chinyanja"}
   {:name "Chinese" :native-name "中文 (Zhōngwén), 汉语, 漢語"}
   {:name "Chuvash" :native-name "чӑваш чӗлхи"}
   {:name "Cornish" :native-name "Kernewek"}
   {:name "Corsican" :native-name "corsu, lingua corsa"}
   {:name "Cree" :native-name "ᓀᐦᐃᔭᐍᐏᐣ"}
   {:name "Croatian" :native-name "hrvatski"}
   {:name "Czech" :native-name "česky, čeština"}
   {:name "Danish" :native-name "dansk"}
   {:name "Divehi; Dhivehi; Maldivian;" :native-name "ދިވެހި"}
   {:name "Dutch" :native-name "Nederlands, Vlaams"}
   {:name "English" :native-name "English"}
   {:name "Esperanto" :native-name "Esperanto"}
   {:name "Estonian" :native-name "eesti, eesti keel"}
   {:name "Ewe" :native-name "Eʋegbe"}
   {:name "Faroese" :native-name "føroyskt"}
   {:name "Fijian" :native-name "vosa Vakaviti"}
   {:name "Finnish" :native-name "suomi, suomen kieli"}
   {:name "French" :native-name "français, langue française"}
   {:name "Fula; Fulah; Pulaar; Pular" :native-name "Fulfulde, Pulaar, Pular"}
   {:name "Galician" :native-name "Galego"}
   {:name "Georgian" :native-name "ქართული"}
   {:name "German" :native-name "Deutsch"}
   {:name "Greek, Modern" :native-name "Ελληνικά"}
   {:name "Guaraní" :native-name "Avañeẽ"}
   {:name "Gujarati" :native-name "ગુજરાતી"}
   {:name "Haitian; Haitian Creole" :native-name "Kreyòl ayisyen"}
   {:name "Hausa" :native-name "Hausa, هَوُسَ"}
   {:name "Hebrew (modern)" :native-name "עברית"}
   {:name "Herero" :native-name "Otjiherero"}
   {:name "Hindi" :native-name "हिन्दी, हिंदी"}
   {:name "Hiri Motu" :native-name "Hiri Motu"}
   {:name "Hungarian" :native-name "Magyar"}
   {:name "Interlingua" :native-name "Interlingua"}
   {:name "Indonesian" :native-name "Bahasa Indonesia"}
   {:name "Interlingue" :native-name "Originally called Occidental; then Interlingue after WWII"}
   {:name "Irish" :native-name "Gaeilge"}
   {:name "Igbo" :native-name "Asụsụ Igbo"}
   {:name "Inupiaq" :native-name "Iñupiaq, Iñupiatun"}
   {:name "Ido" :native-name "Ido"}
   {:name "Icelandic" :native-name "Íslenska"}
   {:name "Italian" :native-name "Italiano"}
   {:name "Inuktitut" :native-name "ᐃᓄᒃᑎᑐᑦ"}
   {:name "Japanese" :native-name "日本語 (にほんご／にっぽんご)"}
   {:name "Javanese" :native-name "basa Jawa"}
   {:name "Kalaallisut, Greenlandic" :native-name "kalaallisut, kalaallit oqaasii"}
   {:name "Kannada" :native-name "ಕನ್ನಡ"}
   {:name "Kanuri" :native-name "Kanuri"}
   {:name "Kashmiri" :native-name "कश्मीरी, كشميري‎"}
   {:name "Kazakh" :native-name "Қазақ тілі"}
   {:name "Khmer" :native-name "ភាសាខ្មែរ"}
   {:name "Kikuyu, Gikuyu" :native-name "Gĩkũyũ"}
   {:name "Kinyarwanda" :native-name "Ikinyarwanda"}
   {:name "Kirghiz, Kyrgyz" :native-name "кыргыз тили"}
   {:name "Komi" :native-name "коми кыв"}
   {:name "Kongo" :native-name "KiKongo"}
   {:name "Korean" :native-name "한국어 (韓國語), 조선말 (朝鮮語)"}
   {:name "Kurdish" :native-name "Kurdî, كوردی‎"}
   {:name "Kwanyama, Kuanyama" :native-name "Kuanyama"}
   {:name "Latin" :native-name "latine, lingua latina"}
   {:name "Luxembourgish, Letzeburgesch" :native-name "Lëtzebuergesch"}
   {:name "Luganda" :native-name "Luganda"}
   {:name "Limburgish, Limburgan, Limburger" :native-name "Limburgs"}
   {:name "Lingala" :native-name "Lingála"}
   {:name "Lao" :native-name "ພາສາລາວ"}
   {:name "Lithuanian" :native-name "lietuvių kalba"}
   {:name "Luba-Katanga" :native-name ""}
   {:name "Latvian" :native-name "latviešu valoda"}
   {:name "Manx" :native-name "Gaelg, Gailck"}
   {:name "Macedonian" :native-name "македонски јазик"}
   {:name "Malagasy" :native-name "Malagasy fiteny"}
   {:name "Malay" :native-name "bahasa Melayu, بهاس ملايو‎"}
   {:name "Malayalam" :native-name "മലയാളം"}
   {:name "Maltese" :native-name "Malti"}
   {:name "Māori" :native-name "te reo Māori"}
   {:name "Marathi (Marāṭhī)" :native-name "मराठी"}
   {:name "Marshallese" :native-name "Kajin M̧ajeļ"}
   {:name "Mongolian" :native-name "монгол"}
   {:name "Nauru" :native-name "Ekakairũ Naoero"}
   {:name "Navajo, Navaho" :native-name "Diné bizaad, Dinékʼehǰí"}
   {:name "Norwegian Bokmål" :native-name "Norsk bokmål"}
   {:name "North Ndebele" :native-name "isiNdebele"}
   {:name "Nepali" :native-name "नेपाली"}
   {:name "Ndonga" :native-name "Owambo"}
   {:name "Norwegian Nynorsk" :native-name "Norsk nynorsk"}
   {:name "Norwegian" :native-name "Norsk"}
   {:name "Nuosu" :native-name "ꆈꌠ꒿ Nuosuhxop"}
   {:name "South Ndebele" :native-name "isiNdebele"}
   {:name "Occitan" :native-name "Occitan"}
   {:name "Ojibwe, Ojibwa" :native-name "ᐊᓂᔑᓈᐯᒧᐎᓐ"}
   {:name "Old Church Slavonic, Church Slavic, Church Slavonic, Old Bulgarian, Old Slavonic" :native-name "ѩзыкъ словѣньскъ"}
   {:name "Oromo" :native-name "Afaan Oromoo"}
   {:name "Oriya" :native-name "ଓଡ଼ିଆ"}
   {:name "Ossetian, Ossetic" :native-name "ирон æвзаг"}
   {:name "Panjabi, Punjabi" :native-name "ਪੰਜਾਬੀ, پنجابی‎"}
   {:name "Pāli" :native-name "पाऴि"}
   {:name "Persian" :native-name "فارسی"}
   {:name "Polish" :native-name "polski"}
   {:name "Pashto, Pushto" :native-name "پښتو"}
   {:name "Portuguese" :native-name "Português"}
   {:name "Quechua" :native-name "Runa Simi, Kichwa"}
   {:name "Romansh" :native-name "rumantsch grischun"}
   {:name "Kirundi" :native-name "kiRundi"}
   {:name "Romanian, Moldavian, Moldovan" :native-name "română"}
   {:name "Russian" :native-name "русский язык"}
   {:name "Sanskrit (Saṁskṛta)" :native-name "संस्कृतम्"}
   {:name "Sardinian" :native-name "sardu"}
   {:name "Sindhi" :native-name "सिन्धी, سنڌي، سندھی‎"}
   {:name "Northern Sami" :native-name "Davvisámegiella"}
   {:name "Samoan" :native-name "gagana faa Samoa"}
   {:name "Sango" :native-name "yângâ tî sängö"}
   {:name "Serbian" :native-name "српски језик"}
   {:name "Scottish Gaelic; Gaelic" :native-name "Gàidhlig"}
   {:name "Shona" :native-name "chiShona"}
   {:name "Sinhala, Sinhalese" :native-name "සිංහල"}
   {:name "Slovak" :native-name "slovenčina"}
   {:name "Slovene" :native-name "slovenščina"}
   {:name "Somali" :native-name "Soomaaliga, af Soomaali"}
   {:name "Southern Sotho" :native-name "Sesotho"}
   {:name "Spanish; Castilian" :native-name "español, castellano"}
   {:name "Sundanese" :native-name "Basa Sunda"}
   {:name "Swahili" :native-name "Kiswahili"}
   {:name "Swati" :native-name "SiSwati"}
   {:name "Swedish" :native-name "svenska"}
   {:name "Tamil" :native-name "தமிழ்"}
   {:name "Telugu" :native-name "తెలుగు"}
   {:name "Tajik" :native-name "тоҷикӣ, toğikī, تاجیکی‎"}
   {:name "Thai" :native-name "ไทย"}
   {:name "Tigrinya" :native-name "ትግርኛ"}
   {:name "Tibetan Standard, Tibetan, Central" :native-name "བོད་ཡིག"}
   {:name "Turkmen" :native-name "Türkmen, Түркмен"}
   {:name "Tagalog" :native-name "Wikang Tagalog, ᜏᜒᜃᜅ᜔ ᜆᜄᜎᜓᜄ᜔"}
   {:name "Tswana" :native-name "Setswana"}
   {:name "Tonga (Tonga Islands)" :native-name "faka Tonga"}
   {:name "Turkish" :native-name "Türkçe"}
   {:name "Tsonga" :native-name "Xitsonga"}
   {:name "Tatar" :native-name "татарча, tatarça, تاتارچا‎"}
   {:name "Twi" :native-name "Twi"}
   {:name "Tahitian" :native-name "Reo Tahiti"}
   {:name "Uighur, Uyghur" :native-name "Uyƣurqə, ئۇيغۇرچە‎"}
   {:name "Ukrainian" :native-name "українська"}
   {:name "Urdu" :native-name "اردو"}
   {:name "Uzbek" :native-name "zbek, Ўзбек, أۇزبېك‎"}
   {:name "Venda" :native-name "Tshivenḓa"}
   {:name "Vietnamese" :native-name "Tiếng Việt"}
   {:name "Volapük" :native-name "Volapük"}
   {:name "Walloon" :native-name "Walon"}
   {:name "Welsh" :native-name "Cymraeg"}
   {:name "Wolof" :native-name "Wollof"}
   {:name "Western Frisian" :native-name "Frysk"}
   {:name "Xhosa" :native-name "isiXhosa"}
   {:name "Yiddish" :native-name "ייִדיש"}
   {:name "Yoruba" :native-name "Yorùbá"}
   {:name "Zhuang, Chuang" :native-name "Saɯ cueŋƅ, Saw cuengh"}])


(def languages
  (map :name language-defs))


(def skills
  ["A/B testing"
   "API development"
   "API integration"
   "AR/VR development"
   "AWS"
   "Accounting"
   "Ad campaign management"
   "Adaptability"
   "AdonisJS"
   "Agile methodology"
   "Agile project management"
   "Airtable"
   "Alpine.js"
   "Analytical skills"
   "Analytics knowledge"
   "Analytics reporting"
   "Angular"
   "Ansible"
   "Apache Kafka"
   "Apache Spark"
   "App store optimization"
   "Artificial intelligence"
   "Assembly"
   "Astro"
   "Attention to deadlines"
   "Attention to detail"
   "Augmented reality"
   "Aurelia"
   "Automate tasks"
   "Azure"
   "B2B sales"
   "Backbone.js"
   "Bash scripting"
   "Blazor"
   "Blockchain development"
   "Blogging"
   "Bootstrap"
   "Brand development"
   "Brand monitoring"
   "Brand strategy"
   "Branding design"
   "Budget management"
   "Bulma CSS"
   "Bun runtime"
   "C"
   "C#"
   "C++"
   "CI/CD pipelines"
   "COBOL"
   "CRM integration"
   "CRM management"
   "CSS"
   "Chakra UI"
   "ChatGPT integration"
   "Claude API integration"
   "Client communication"
   "Client management"
   "Client retention"
   "Client satisfaction"
   "Clojure / ClojureScript"
   "Cloud computing"
   "CockroachDB"
   "Code debugging"
   "CodeIgniter"
   "Coding proficiency"
   "Coding"
   "CoffeeScript"
   "Cognitive computing strategies"
   "Collaboration skills"
   "Communication planning"
   "Communication"
   "Competitive analysis"
   "Computer programming knowledge"
   "Computer vision"
   "Conceptual thinking"
   "Conflict resolution"
   "Container orchestration"
   "Content curation"
   "Content management"
   "Content strategy"
   "Content writing"
   "Contract negotiation"
   "Conversion optimization"
   "Copy editing"
   "Copywriting"
   "Cost analysis"
   "Creative thinking"
   "Creative writing"
   "Crisis management"
   "Critical thinking"
   "Crystal"
   "Cryptocurrency trading"
   "Cucumber"
   "Customer engagement"
   "Customer feedback"
   "Customer retention"
   "Customer service"
   "Cybersecurity"
   "Cypress"
   "D"
   "DApp development"
   "Dart"
   "Data analysis"
   "Data cleaning"
   "Data entry accuracy"
   "Data management"
   "Data migration"
   "Data science"
   "Data visualization"
   "Database design"
   "Database management"
   "Database programming"
   "DeFi protocols"
   "Deep learning"
   "Delphi"
   "Deno"
   "DevOps"
   "Digital advertising"
   "Digital asset management"
   "Digital content creation"
   "Digital marketing"
   "Digital prototyping"
   "Digital strategy"
   "Discord bot development"
   "Django"
   "Docker"
   "Drupal"
   "E-commerce management"
   "ETL processes"
   "Edge computing"
   "Electron apps"
   "Elixir"
   "Elm"
   "Email automation"
   "Email campaigns"
   "Email management"
   "Email marketing"
   "Email security"
   "Ember.js"
   "Erlang"
   "Ethereum development"
   "Event planning"
   "Excel proficiency"
   "Experience with CRM"
   "Express.js"
   "F#"
   "FastAPI"
   "Fastify"
   "Figma design"
   "File management skills"
   "Financial analysis"
   "Financial reporting"
   "Flask"
   "Flutter"
   "Fluency in multiple languages"
   "Fortran"
   "Foundation CSS"
   "Framer development"
   "GCP (Google Cloud)"
   "GPT fine-tuning"
   "GPT prompt engineering"
   "Game development"
   "Gatsby.js"
   "Gemini API"
   "Gin framework"
   "Git version control"
   "GitHub Actions"
   "Go (Golang)"
   "Gradle"
   "Grails"
   "GraphQL"
   "Graphic design"
   "Groovy"
   "Gulp.js"
   "HTML"
   "Hack"
   "Hadoop"
   "Hapi.js"
   "Haskell"
   "Headless CMS"
   "Hibernate"
   "Hugo"
   "Hyperledger"
   "IT architecture"
   "IT compliance knowledge"
   "IT hardware troubleshooting"
   "IT inventory management"
   "IT networking"
   "IT policy knowledge"
   "IT project management"
   "IT risk management"
   "IT security awareness"
   "IT software proficiency"
   "IT support skills"
   "IT troubleshooting"
   "Idris"
   "Image editing"
   "Image generation AI"
   "Influencer marketing"
   "Influencer outreach"
   "Internet research abilities"
   "Inventory management"
   "Ionic"
   "JAMstack development"
   "JQuery"
   "Julia"
   "Jasmine"
   "Java"
   "JavaScript"
   "Jekyll"
   "Jenkins"
   "Jest"
   "Joomla"
   "Jupyter notebooks"
   "Keras"
   "Koa.js"
   "Kotlin"
   "Kubernetes"
   "LLM integration"
   "LaTeX"
   "LangChain"
   "Laravel"
   "Lead generation"
   "Leadership qualities"
   "Less CSS"
   "Linux administration"
   "Lisp"
   "Lit"
   "Lua"
   "Low-code platforms"
   "MATLAB"
   "MUI (Material-UI)"
   "Machine learning"
   "Mantine"
   "Market positioning"
   "Market research"
   "Market segmentation"
   "Market trends"
   "Marketing automation"
   "Material Design"
   "Maven"
   "Mental arithmetic"
   "Metaverse development"
   "Meteor.js"
   "Micronaut"
   "Microservices"
   "Microsoft Office proficiency"
   "Midjourney prompting"
   "Mobile app development"
   "Mobile marketing"
   "Mobile optimization"
   "Mocha"
   "MongoDB"
   "Multi-channel marketing"
   "Multitasking ability"
   "MySQL"
   "NFT creation"
   "NFT marketplace development"
   "Natural language processing"
   "Negotiation"
   "Neo4j"
   "Nest.js"
   "Network protocols"
   "Networking"
   "Neural network design"
   "Next.js"
   "Nginx"
   "Nim"
   "No-code development"
   "Node.js"
   "Notion management"
   "NumPy"
   "Nuxt.js"
   "OAuth implementation"
   "Objective-C"
   "Ocaml"
   "Online community management"
   "Online fundraising"
   "Online reputation management"
   "Online research"
   "Online sales"
   "Online scheduling"
   "Online training"
   "OpenAI API"
   "OpenGL"
   "Organizational skills"
   "PHP"
   "Pandas"
   "Parcel"
   "Pascal"
   "Performance evaluation"
   "Performance tracking"
   "Perl"
   "Phoenix framework"
   "Photo editing"
   "Playwright"
   "Polymer"
   "PostgreSQL"
   "PowerShell"
   "Preact"
   "Predictive analytics"
   "Present complex information"
   "Presentation design"
   "Prisma ORM"
   "Problem solving"
   "Processing"
   "Product launches"
   "Product management"
   "Product photography"
   "Productivity tools"
   "Programming"
   "Progressive web apps"
   "Project coordination"
   "Project management"
   "Prolog"
   "Prompt engineering"
   "Protractor"
   "Public speaking"
   "Pug"
   "Puppet"
   "PyTorch"
   "Python"
   "Qiskit"
   "Qt framework"
   "Quarkus"
   "Quality assurance"
   "R"
   "RAG systems"
   "REST API design"
   "RSpec"
   "Racket"
   "React Native"
   "React.js"
   "ReasonML"
   "Redis"
   "Redux"
   "Reinforcement learning"
   "Remix framework"
   "Remote communication"
   "Remote team management"
   "Remote training"
   "Remote work tools"
   "Report writing"
   "Research"
   "Responsive design"
   "Rollup"
   "Ruby"
   "Ruby on Rails"
   "Rust"
   "SEO auditing"
   "SEO optimization"
   "SEO strategy"
   "SQL"
   "SQLAlchemy"
   "SQLite"
   "Sails.js"
   "Sales"
   "Sanity CMS"
   "Sass/SCSS"
   "Scala"
   "Scheme"
   "Scikit-learn"
   "Security protocols"
   "Selenium"
   "Semantic UI"
   "Server management"
   "Serverless architecture"
   "Shopify development"
   "Sinatra"
   "Smalltalk"
   "Smart contract auditing"
   "Smart contract development"
   "Social media advertising"
   "Social media analytics"
   "Social media engagement"
   "Social media management"
   "Software integration"
   "Software licensing"
   "Solana development"
   "Solid.js"
   "Solidity"
   "Speech recognition"
   "Spreadsheets"
   "Spring Boot"
   "Spring Framework"
   "Stable Diffusion"
   "Stimulus"
   "Storybook"
   "Strategy development"
   "Strapi"
   "Stripe integration"
   "Strong typing skills"
   "Stylus CSS"
   "Supabase"
   "Supplier relations"
   "Supply chain management"
   "Svelte/SvelteKit"
   "Swift"
   "Symfony"
   "System architecture"
   "Systems analysis"
   "Tailwind CSS"
   "Tcl"
   "Team building"
   "Team collaboration"
   "Team leadership"
   "Teamwork"
   "Technical proficiency"
   "Technical writing"
   "Telegram bot development"
   "TensorFlow"
   "Terraform"
   "Three.js"
   "Time management"
   "Time tracking"
   "Token economics"
   "Trend analysis"
   "Troubleshoot hardware issues"
   "Turbo"
   "TypeORM"
   "TypeScript"
   "UI/UX design"
   "UX design"
   "Unity development"
   "Unreal Engine"
   "User retention"
   "User testing"
   "V"
   "VHDL"
   "Vala"
   "Vector databases"
   "Vendor management"
   "Verilog"
   "Video conferencing"
   "Video editing"
   "Video marketing"
   "Video production"
   "Virtual collaboration"
   "Virtual event hosting"
   "Virtual event planning"
   "Virtual team management"
   "Virtualization"
   "Visual Basic"
   "Vite"
   "Voice AI integration"
   "Vue.js"
   "Vuetify"
   "WASM development"
   "Web Components"
   "Web3 development"
   "Web accessibility"
   "Web analytics"
   "Web content management"
   "Web design"
   "Web development"
   "Web scraping"
   "WebAssembly"
   "WebGL"
   "WebRTC"
   "Webflow development"
   "Webpack"
   "Website migration"
   "Website optimization"
   "Website security"
   "WordPress"
   "Work independently"
   "Work under pressure"
   "Xamarin"
   "Yii framework"
   "Zapier automation"
   "Zend Framework"
   "Zero-knowledge proofs"
   "Zig"
   "Zustand"
   "iOS development"
   "jQuery Mobile"
   "tRPC"])


(def categories
  #{"Accounting & Consulting"
    "Admin Support"
    "Customer Service"
    "Data Science & Analytics"
    "Design & Creative"
    "IT & Networking"
    "Legal"
    "Other"
    "Sales & Marketing"
    "Translation"
    "Web, Mobile & Software Dev"
    "Writing"})


(def category-default "All Categories")


(def categories-with-default
  (conj (map #(vector % %) categories) ["All Categories" nil]))
