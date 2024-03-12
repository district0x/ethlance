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
   "API integration"
   "Accounting"
   "Ad campaign management"
   "Adaptability"
   "Agile methodology"
   "Agile project management"
   "Analytical skills"
   "Analytics knowledge"
   "Analytics reporting"
   "App store optimization"
   "Attention to deadlines"
   "Attention to detail"
   "Automate tasks"
   "B2B sales"
   "Blogging"
   "Brand development"
   "Brand monitoring"
   "Brand strategy"
   "Branding design"
   "Budget management"
   "C++"
   "CRM integration"
   "CRM management"
   "CSS"
   "ChatGPT and similar"
   "Client communication"
   "Client management"
   "Client retention"
   "Client satisfaction"
   "Clojure / ClojureScript"
   "Cloud computing"
   "Code debugging"
   "Coding proficiency"
   "Coding"
   "Cognitive computing strategies"
   "Collaboration skills"
   "Communication planning"
   "Communication"
   "Competitive analysis"
   "Computer programming knowledge"
   "Computer vision techniques"
   "Conceptual thinking"
   "Conflict resolution"
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
   "Customer engagement"
   "Customer feedback"
   "Customer retention"
   "Customer service"
   "Cybersecurity"
   "Data analysis"
   "Data analysis"
   "Data analysis"
   "Data cleaning"
   "Data entry accuracy"
   "Data management"
   "Data migration"
   "Data visualization"
   "Data visualization"
   "Database design"
   "Database management"
   "Database programming"
   "Deep learning model"
   "Digital advertising"
   "Digital asset management"
   "Digital content creation"
   "Digital marketing"
   "Digital prototyping"
   "Digital strategy"
   "E-commerce management"
   "Email automation"
   "Email campaigns"
   "Email management"
   "Email marketing"
   "Email security"
   "Event planning"
   "Excel proficiency"
   "Experience with CRM"
   "File management skills"
   "Financial analysis"
   "Financial reporting"
   "Fluency in multiple languages"
   "Graphic design"
   "HTML"
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
   "Image editing"
   "Influencer marketing"
   "Influencer outreach"
   "Internet research abilities"
   "Inventory management"
   "Java"
   "JavaScript"
   "Lead generation"
   "Leadership qualities"
   "Machine learning algorithms"
   "Market positioning"
   "Market research"
   "Market segmentation"
   "Market trends"
   "Marketing automation"
   "Mental arithmetic"
   "Microsoft Office proficiency"
   "Mobile app development"
   "Mobile marketing"
   "Mobile optimization"
   "Multi-channel marketing"
   "Multitasking ability"
   "Natural language processing"
   "Negotiation"
   "Network protocols"
   "Networking"
   "Neural network design"
   "Online community management"
   "Online fundraising"
   "Online reputation management"
   "Online research"
   "Online sales"
   "Online scheduling"
   "Online training"
   "Organizational skills"
   "Performance evaluation"
   "Performance tracking"
   "Photo editing"
   "Predictive analytics techniques"
   "Present complex information"
   "Presentation design"
   "Problem solving"
   "Product launches"
   "Product management"
   "Product photography"
   "Productivity tools"
   "Programming"
   "Project coordination"
   "Project management"
   "Project management"
   "Public speaking"
   "Quality assurance"
   "Reinforcement learning algorithms"
   "Remote communication"
   "Remote team management"
   "Remote training"
   "Remote work tools"
   "Report writing"
   "Research"
   "Responsive design"
   "SEO auditing"
   "SEO optimization"
   "SEO strategy"
   "SQL"
   "Sales"
   "Security protocols"
   "Server management"
   "Social media advertising"
   "Social media analytics"
   "Social media engagement"
   "Social media management"
   "Software integration"
   "Software licensing"
   "Speech recognition technology"
   "Spreadsheets"
   "Stable diffusion"
   "Strategy development"
   "Strong typing skills"
   "Supplier relations"
   "Supply chain management"
   "System architecture"
   "Systems analysis"
   "Team building"
   "Team collaboration"
   "Team leadership"
   "Teamwork"
   "Technical proficiency"
   "Time management"
   "Time tracking"
   "Trend analysis"
   "Troubleshoot hardware issues"
   "UI/UX design"
   "UX design"
   "User retention"
   "User testing"
   "Vendor management"
   "Video conferencing"
   "Video editing"
   "Video marketing"
   "Video production"
   "Virtual collaboration"
   "Virtual event hosting"
   "Virtual event planning"
   "Virtual team management"
   "Virtualization"
   "Web accessibility"
   "Web analytics"
   "Web content management"
   "Web design"
   "Web development"
   "Website migration"
   "Website optimization"
   "Website security"
   "WordPress"
   "Work independently"
   "Work under pressure"])


(def categories
  #{"Web, Mobile & Software Dev"
    "IT & Networking"
    "Data Science & Analytics"
    "Design & Creative"
    "Writing"
    "Translation"
    "Legal"
    "Admin Support"
    "Customer Service"
    "Sales & Marketing"
    "Accounting & Consulting"
    "Other"})


(def category-default "All Categories")


(def categories-with-default
  (conj (map #(vector % %) categories) ["All Categories" nil]))
