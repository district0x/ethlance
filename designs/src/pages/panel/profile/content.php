<div class="content profile with-tabs">
    <div class="geral-buttons-opts-screen"> 
        <div class="flex ">
            <a class="buttons-tabs-screen active"><span>Candidates Profile</span></a>
            <a class="buttons-tabs-screen"><span>Employer Profile</span></a>
            <a class="buttons-tabs-screen"><span>Arbiter Profile</span></a>
        </div>
        <div class="only-mobile combobox">
            <select>
                <option value="0">Candidates Profile</option>
                <option value="1">Employer Profile</option>
                <option value="2">Arbiter Profile</option>
            </select>
            <div class="items"></div>
        </div>
        <span class="icon-opt-title"><img src="./images/svg/ic-white-arrow-down.svg" alt=""></span>
    </div>
    <?php include ('./candidates.php'); ?>
    <?php include ('./employer.php'); ?>
    <?php include ('./arbiter.php'); ?>
</div>
