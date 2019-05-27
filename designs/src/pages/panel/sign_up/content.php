<div class="content sign-up with-tabs">
    <div class="geral-buttons-opts-screen">
        <div class="flex">
            <a class="buttons-tabs-screen active"><span>Candidates</span></a>
            <a class="buttons-tabs-screen"><span>Employer</span></a>
            <a class="buttons-tabs-screen"><span>Arbiter</span></a>
        </div>
        <div class="only-mobile combobox">
            <select>
                <option value="0">Candidates</option>
                <option value="1">Employer</option>
                <option value="2">Arbiter</option>
            </select>
            <div class="items"></div>
        </div>
        <span class="icon-opt-title"><img src="./images/svg/ic-white-arrow-down.svg" alt=""></span>
    </div>
    <div class="box-content box-shadow">
        <?php include ('./candidates.php'); ?>
        <?php include ('./employer.php'); ?>
        <?php include ('./arbiter.php'); ?>
    </div>
</div>