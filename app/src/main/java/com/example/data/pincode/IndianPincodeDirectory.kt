package com.example.data.pincode

data class PincodeLocation(
    val cityOrDistrict: String,
    val state: String,
    val isExactMatch: Boolean = true
) {
    val displayName: String
        get() = if (cityOrDistrict.isNotEmpty()) "$cityOrDistrict, $state" else state
}

/**
 * Offline Indian Postal Pincode Directory.
 * Maps 6-digit Indian PIN codes to City/District and State without needing an internet connection.
 */
object IndianPincodeDirectory {

    // 3-digit Prefix Mapping (Major Cities & Postal Hubs)
    private val prefix3Map = mapOf(
        // West Bengal & East
        "700" to PincodeLocation("Kolkata", "West Bengal"),
        "711" to PincodeLocation("Howrah", "West Bengal"),
        "712" to PincodeLocation("Hooghly", "West Bengal"),
        "713" to PincodeLocation("Burdwan / Durgapur", "West Bengal"),
        "721" to PincodeLocation("Midnapore", "West Bengal"),
        "722" to PincodeLocation("Bankura", "West Bengal"),
        "723" to PincodeLocation("Purulia", "West Bengal"),
        "731" to PincodeLocation("Birbhum / Bolpur", "West Bengal"),
        "732" to PincodeLocation("Malda", "West Bengal"),
        "733" to PincodeLocation("Uttar Dinajpur", "West Bengal"),
        "734" to PincodeLocation("Siliguri / Darjeeling", "West Bengal"),
        "735" to PincodeLocation("Jalpaiguri", "West Bengal"),
        "736" to PincodeLocation("Cooch Behar", "West Bengal"),
        "741" to PincodeLocation("Nadia / Kalyani", "West Bengal"),
        "742" to PincodeLocation("Murshidabad", "West Bengal"),
        "743" to PincodeLocation("North / South 24 Parganas", "West Bengal"),
        "744" to PincodeLocation("Port Blair", "Andaman & Nicobar Islands"),

        // Delhi & NCR
        "110" to PincodeLocation("New Delhi", "Delhi"),
        "111" to PincodeLocation("South Delhi", "Delhi"),
        "201" to PincodeLocation("Noida / Ghaziabad", "Uttar Pradesh"),
        "122" to PincodeLocation("Gurugram", "Haryana"),
        "121" to PincodeLocation("Faridabad", "Haryana"),

        // Maharashtra & Goa
        "400" to PincodeLocation("Mumbai", "Maharashtra"),
        "401" to PincodeLocation("Thane / Palghar", "Maharashtra"),
        "402" to PincodeLocation("Raigad", "Maharashtra"),
        "403" to PincodeLocation("Panaji", "Goa"),
        "411" to PincodeLocation("Pune", "Maharashtra"),
        "412" to PincodeLocation("Pune Rural", "Maharashtra"),
        "413" to PincodeLocation("Solapur", "Maharashtra"),
        "414" to PincodeLocation("Ahmednagar", "Maharashtra"),
        "415" to PincodeLocation("Satara / Ratnagiri", "Maharashtra"),
        "416" to PincodeLocation("Kolhapur / Sangli", "Maharashtra"),
        "421" to PincodeLocation("Kalyan / Dombivli", "Maharashtra"),
        "422" to PincodeLocation("Nashik", "Maharashtra"),
        "423" to PincodeLocation("Malegaon", "Maharashtra"),
        "424" to PincodeLocation("Dhule", "Maharashtra"),
        "425" to PincodeLocation("Jalgaon", "Maharashtra"),
        "431" to PincodeLocation("Chhatrapati Sambhajinagar", "Maharashtra"),
        "440" to PincodeLocation("Nagpur", "Maharashtra"),
        "441" to PincodeLocation("Nagpur Rural", "Maharashtra"),
        "442" to PincodeLocation("Wardha", "Maharashtra"),
        "444" to PincodeLocation("Amravati / Akola", "Maharashtra"),

        // Karnataka
        "560" to PincodeLocation("Bengaluru", "Karnataka"),
        "561" to PincodeLocation("Bengaluru Rural", "Karnataka"),
        "562" to PincodeLocation("Kolar / Ramanagara", "Karnataka"),
        "563" to PincodeLocation("Kolar Gold Fields", "Karnataka"),
        "570" to PincodeLocation("Mysuru", "Karnataka"),
        "571" to PincodeLocation("Chamarajanagar", "Karnataka"),
        "572" to PincodeLocation("Tumakuru", "Karnataka"),
        "573" to PincodeLocation("Hassan", "Karnataka"),
        "574" to PincodeLocation("Dakshina Kannada", "Karnataka"),
        "575" to PincodeLocation("Mangaluru", "Karnataka"),
        "576" to PincodeLocation("Udupi", "Karnataka"),
        "577" to PincodeLocation("Shivamogga / Davanagere", "Karnataka"),
        "580" to PincodeLocation("Hubballi / Dharwad", "Karnataka"),
        "581" to PincodeLocation("Uttara Kannada", "Karnataka"),
        "582" to PincodeLocation("Gadag", "Karnataka"),
        "583" to PincodeLocation("Ballari / Vijayanagara", "Karnataka"),
        "584" to PincodeLocation("Raichur", "Karnataka"),
        "585" to PincodeLocation("Kalaburagi / Bidar", "Karnataka"),
        "586" to PincodeLocation("Vijayapura", "Karnataka"),
        "587" to PincodeLocation("Bagalkote", "Karnataka"),
        "590" to PincodeLocation("Belagavi", "Karnataka"),

        // Tamil Nadu & Puducherry
        "600" to PincodeLocation("Chennai", "Tamil Nadu"),
        "601" to PincodeLocation("Tiruvallur", "Tamil Nadu"),
        "602" to PincodeLocation("Kanchipuram", "Tamil Nadu"),
        "603" to PincodeLocation("Chengalpattu", "Tamil Nadu"),
        "604" to PincodeLocation("Villupuram", "Tamil Nadu"),
        "605" to PincodeLocation("Puducherry", "Puducherry"),
        "606" to PincodeLocation("Tiruvannamalai", "Tamil Nadu"),
        "607" to PincodeLocation("Cuddalore", "Tamil Nadu"),
        "620" to PincodeLocation("Tiruchirappalli", "Tamil Nadu"),
        "624" to PincodeLocation("Dindigul", "Tamil Nadu"),
        "625" to PincodeLocation("Madurai", "Tamil Nadu"),
        "626" to PincodeLocation("Virudhunagar", "Tamil Nadu"),
        "627" to PincodeLocation("Tirunelveli", "Tamil Nadu"),
        "628" to PincodeLocation("Thoothukudi", "Tamil Nadu"),
        "629" to PincodeLocation("Kanyakumari", "Tamil Nadu"),
        "632" to PincodeLocation("Vellore", "Tamil Nadu"),
        "635" to PincodeLocation("Krishnagiri", "Tamil Nadu"),
        "636" to PincodeLocation("Salem", "Tamil Nadu"),
        "637" to PincodeLocation("Namakkal", "Tamil Nadu"),
        "638" to PincodeLocation("Erode", "Tamil Nadu"),
        "641" to PincodeLocation("Coimbatore", "Tamil Nadu"),
        "642" to PincodeLocation("Pollachi", "Tamil Nadu"),
        "643" to PincodeLocation("Nilgiris / Ooty", "Tamil Nadu"),

        // Kerala
        "682" to PincodeLocation("Kochi / Ernakulam", "Kerala"),
        "683" to PincodeLocation("Aluva / Ernakulam", "Kerala"),
        "680" to PincodeLocation("Thrissur", "Kerala"),
        "686" to PincodeLocation("Kottayam", "Kerala"),
        "688" to PincodeLocation("Alappuzha", "Kerala"),
        "689" to PincodeLocation("Pathanamthitta", "Kerala"),
        "690" to PincodeLocation("Kollam North", "Kerala"),
        "691" to PincodeLocation("Kollam", "Kerala"),
        "695" to PincodeLocation("Thiruvananthapuram", "Kerala"),
        "670" to PincodeLocation("Kannur", "Kerala"),
        "671" to PincodeLocation("Kasaragod", "Kerala"),
        "673" to PincodeLocation("Kozhikode", "Kerala"),
        "676" to PincodeLocation("Malappuram", "Kerala"),
        "678" to PincodeLocation("Palakkad", "Kerala"),
        "679" to PincodeLocation("Shornur / Palakkad", "Kerala"),

        // Telangana & Andhra Pradesh
        "500" to PincodeLocation("Hyderabad", "Telangana"),
        "501" to PincodeLocation("Ranga Reddy", "Telangana"),
        "502" to PincodeLocation("Medak / Sangareddy", "Telangana"),
        "503" to PincodeLocation("Nizamabad", "Telangana"),
        "504" to PincodeLocation("Adilabad", "Telangana"),
        "505" to PincodeLocation("Karimnagar", "Telangana"),
        "506" to PincodeLocation("Warangal", "Telangana"),
        "507" to PincodeLocation("Khammam", "Telangana"),
        "508" to PincodeLocation("Nalgonda", "Telangana"),
        "509" to PincodeLocation("Mahbubnagar", "Telangana"),
        "515" to PincodeLocation("Anantapur", "Andhra Pradesh"),
        "516" to PincodeLocation("Kadapa", "Andhra Pradesh"),
        "517" to PincodeLocation("Tirupati / Chittoor", "Andhra Pradesh"),
        "518" to PincodeLocation("Kurnool", "Andhra Pradesh"),
        "520" to PincodeLocation("Vijayawada", "Andhra Pradesh"),
        "521" to PincodeLocation("Krishna", "Andhra Pradesh"),
        "522" to PincodeLocation("Guntur", "Andhra Pradesh"),
        "523" to PincodeLocation("Prakasam", "Andhra Pradesh"),
        "524" to PincodeLocation("Nellore", "Andhra Pradesh"),
        "530" to PincodeLocation("Visakhapatnam", "Andhra Pradesh"),
        "531" to PincodeLocation("Anakapalli", "Andhra Pradesh"),
        "532" to PincodeLocation("Srikakulam", "Andhra Pradesh"),
        "533" to PincodeLocation("East Godavari / Kakinada", "Andhra Pradesh"),
        "534" to PincodeLocation("West Godavari / Eluru", "Andhra Pradesh"),
        "535" to PincodeLocation("Vizianagaram", "Andhra Pradesh"),

        // Gujarat
        "380" to PincodeLocation("Ahmedabad", "Gujarat"),
        "382" to PincodeLocation("Gandhinagar", "Gujarat"),
        "390" to PincodeLocation("Vadodara", "Gujarat"),
        "395" to PincodeLocation("Surat", "Gujarat"),
        "360" to PincodeLocation("Rajkot", "Gujarat"),
        "361" to PincodeLocation("Jamnagar", "Gujarat"),
        "364" to PincodeLocation("Bhavnagar", "Gujarat"),
        "365" to PincodeLocation("Amreli", "Gujarat"),
        "362" to PincodeLocation("Junagadh", "Gujarat"),
        "370" to PincodeLocation("Kutch / Bhuj", "Gujarat"),
        "384" to PincodeLocation("Mehsana", "Gujarat"),
        "388" to PincodeLocation("Anand", "Gujarat"),
        "396" to PincodeLocation("Valsad / Navsari", "Gujarat"),

        // Rajasthan
        "302" to PincodeLocation("Jaipur", "Rajasthan"),
        "301" to PincodeLocation("Alwar", "Rajasthan"),
        "303" to PincodeLocation("Jaipur Rural", "Rajasthan"),
        "305" to PincodeLocation("Ajmer", "Rajasthan"),
        "311" to PincodeLocation("Bhilwara", "Rajasthan"),
        "313" to PincodeLocation("Udaipur", "Rajasthan"),
        "324" to PincodeLocation("Kota", "Rajasthan"),
        "334" to PincodeLocation("Bikaner", "Rajasthan"),
        "342" to PincodeLocation("Jodhpur", "Rajasthan"),

        // Uttar Pradesh & Uttarakhand
        "226" to PincodeLocation("Lucknow", "Uttar Pradesh"),
        "208" to PincodeLocation("Kanpur", "Uttar Pradesh"),
        "221" to PincodeLocation("Varanasi", "Uttar Pradesh"),
        "211" to PincodeLocation("Prayagraj", "Uttar Pradesh"),
        "282" to PincodeLocation("Agra", "Uttar Pradesh"),
        "281" to PincodeLocation("Mathura", "Uttar Pradesh"),
        "250" to PincodeLocation("Meerut", "Uttar Pradesh"),
        "243" to PincodeLocation("Bareilly", "Uttar Pradesh"),
        "244" to PincodeLocation("Moradabad", "Uttar Pradesh"),
        "247" to PincodeLocation("Saharanpur", "Uttar Pradesh"),
        "248" to PincodeLocation("Dehradun", "Uttarakhand"),
        "249" to PincodeLocation("Haridwar / Rishikesh", "Uttarakhand"),
        "263" to PincodeLocation("Nainital / Haldwani", "Uttarakhand"),

        // Madhya Pradesh & Chhattisgarh
        "462" to PincodeLocation("Bhopal", "Madhya Pradesh"),
        "452" to PincodeLocation("Indore", "Madhya Pradesh"),
        "474" to PincodeLocation("Gwalior", "Madhya Pradesh"),
        "482" to PincodeLocation("Jabalpur", "Madhya Pradesh"),
        "456" to PincodeLocation("Ujjain", "Madhya Pradesh"),
        "492" to PincodeLocation("Raipur", "Chhattisgarh"),
        "490" to PincodeLocation("Bhilai / Durg", "Chhattisgarh"),
        "495" to PincodeLocation("Bilaspur", "Chhattisgarh"),

        // Bihar & Jharkhand
        "800" to PincodeLocation("Patna", "Bihar"),
        "801" to PincodeLocation("Danapur / Patna", "Bihar"),
        "803" to PincodeLocation("Nalanda", "Bihar"),
        "823" to PincodeLocation("Gaya", "Bihar"),
        "812" to PincodeLocation("Bhagalpur", "Bihar"),
        "842" to PincodeLocation("Muzaffarpur", "Bihar"),
        "846" to PincodeLocation("Darbhanga", "Bihar"),
        "834" to PincodeLocation("Ranchi", "Jharkhand"),
        "831" to PincodeLocation("Jamshedpur", "Jharkhand"),
        "826" to PincodeLocation("Dhanbad", "Jharkhand"),
        "827" to PincodeLocation("Bokaro", "Jharkhand"),

        // Odisha
        "751" to PincodeLocation("Bhubaneswar", "Odisha"),
        "753" to PincodeLocation("Cuttack", "Odisha"),
        "752" to PincodeLocation("Puri", "Odisha"),
        "760" to PincodeLocation("Berhampur", "Odisha"),
        "769" to PincodeLocation("Rourkela", "Odisha"),
        "768" to PincodeLocation("Sambalpur", "Odisha"),

        // Assam & North East
        "781" to PincodeLocation("Guwahati", "Assam"),
        "782" to PincodeLocation("Nagaon", "Assam"),
        "784" to PincodeLocation("Tezpur", "Assam"),
        "785" to PincodeLocation("Jorhat", "Assam"),
        "786" to PincodeLocation("Dibrugarh", "Assam"),
        "788" to PincodeLocation("Silchar", "Assam"),
        "793" to PincodeLocation("Shillong", "Meghalaya"),
        "799" to PincodeLocation("Agartala", "Tripura"),
        "796" to PincodeLocation("Aizawl", "Mizoram"),
        "795" to PincodeLocation("Imphal", "Manipur"),
        "797" to PincodeLocation("Kohima / Dimapur", "Nagaland"),
        "791" to PincodeLocation("Itanagar", "Arunachal Pradesh"),
        "737" to PincodeLocation("Gangtok", "Sikkim"),

        // Punjab, Haryana, HP, J&K
        "160" to PincodeLocation("Chandigarh", "Chandigarh"),
        "141" to PincodeLocation("Ludhiana", "Punjab"),
        "143" to PincodeLocation("Amritsar", "Punjab"),
        "144" to PincodeLocation("Jalandhar", "Punjab"),
        "147" to PincodeLocation("Patiala", "Punjab"),
        "151" to PincodeLocation("Bathinda", "Punjab"),
        "133" to PincodeLocation("Ambala", "Haryana"),
        "132" to PincodeLocation("Karnal / Panipat", "Haryana"),
        "134" to PincodeLocation("Panchkula", "Haryana"),
        "131" to PincodeLocation("Sonipat", "Haryana"),
        "124" to PincodeLocation("Rohtak", "Haryana"),
        "125" to PincodeLocation("Hisar", "Haryana"),
        "171" to PincodeLocation("Shimla", "Himachal Pradesh"),
        "176" to PincodeLocation("Dharamshala / Kangra", "Himachal Pradesh"),
        "180" to PincodeLocation("Jammu", "Jammu & Kashmir"),
        "190" to PincodeLocation("Srinagar", "Jammu & Kashmir"),
        "194" to PincodeLocation("Leh", "Ladakh")
    )

    // 2-digit Sub-zone fallback mapping (State / Postal Circle)
    private val prefix2Map = mapOf(
        "11" to "Delhi",
        "12" to "Haryana",
        "13" to "Haryana",
        "14" to "Punjab",
        "15" to "Punjab",
        "16" to "Chandigarh",
        "17" to "Himachal Pradesh",
        "18" to "Jammu & Kashmir",
        "19" to "Jammu & Kashmir / Ladakh",
        "20" to "Uttar Pradesh",
        "21" to "Uttar Pradesh",
        "22" to "Uttar Pradesh",
        "23" to "Uttar Pradesh",
        "24" to "Uttarakhand / UP",
        "25" to "Uttar Pradesh",
        "26" to "Uttarakhand / UP",
        "27" to "Uttar Pradesh",
        "28" to "Uttar Pradesh",
        "30" to "Rajasthan",
        "31" to "Rajasthan",
        "32" to "Rajasthan",
        "33" to "Rajasthan",
        "34" to "Rajasthan",
        "36" to "Gujarat",
        "37" to "Gujarat",
        "38" to "Gujarat",
        "39" to "Gujarat",
        "40" to "Maharashtra / Goa",
        "41" to "Maharashtra",
        "42" to "Maharashtra",
        "43" to "Maharashtra",
        "44" to "Maharashtra",
        "45" to "Madhya Pradesh",
        "46" to "Madhya Pradesh",
        "47" to "Madhya Pradesh",
        "48" to "Madhya Pradesh",
        "49" to "Chhattisgarh",
        "50" to "Telangana",
        "51" to "Andhra Pradesh",
        "52" to "Andhra Pradesh",
        "53" to "Andhra Pradesh",
        "56" to "Karnataka",
        "57" to "Karnataka",
        "58" to "Karnataka",
        "59" to "Karnataka",
        "60" to "Tamil Nadu",
        "61" to "Tamil Nadu",
        "62" to "Tamil Nadu",
        "63" to "Tamil Nadu",
        "64" to "Tamil Nadu",
        "67" to "Kerala",
        "68" to "Kerala",
        "69" to "Kerala",
        "70" to "West Bengal",
        "71" to "West Bengal",
        "72" to "West Bengal",
        "73" to "West Bengal / Sikkim",
        "74" to "West Bengal",
        "75" to "Odisha",
        "76" to "Odisha",
        "77" to "Odisha",
        "78" to "Assam",
        "79" to "North Eastern Circle",
        "80" to "Bihar",
        "81" to "Bihar / Jharkhand",
        "82" to "Jharkhand",
        "83" to "Jharkhand",
        "84" to "Bihar",
        "85" to "Bihar"
    )

    /**
     * Resolves a 6-digit Indian PIN code to its destination City/District and State.
     */
    fun lookup(pincode: String): PincodeLocation? {
        val clean = pincode.trim().filter { it.isDigit() }
        if (clean.length != 6) return null

        val p3 = clean.substring(0, 3)
        prefix3Map[p3]?.let { return it }

        val p2 = clean.substring(0, 2)
        prefix2Map[p2]?.let { state ->
            return PincodeLocation(
                cityOrDistrict = "",
                state = state,
                isExactMatch = false
            )
        }

        return null
    }
}
